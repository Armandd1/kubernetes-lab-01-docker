#include <drogon/drogon.h>

#include <cctype>
#include <functional>
#include <mutex>
#include <random>
#include <string>
#include <unordered_map>

using namespace drogon;

namespace {

constexpr const char *kAlphabet =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

// In-memory storage only. No database, no external services.
std::unordered_map<std::string, std::string> g_pastes;
std::mutex g_mutex;
std::mt19937_64 g_rng{std::random_device{}()};

std::string generateCode()
{
    std::uniform_int_distribution<std::size_t> dist(0, 61);
    std::string code;
    code.reserve(8);
    for (int i = 0; i < 8; ++i)
    {
        code.push_back(kAlphabet[dist(g_rng)]);
    }
    return code;
}

bool isHttpUrl(const std::string &url)
{
    return url.rfind("http://", 0) == 0 || url.rfind("https://", 0) == 0;
}

bool isCodeShape(const std::string &code)
{
    if (code.size() != 8)
    {
        return false;
    }
    for (unsigned char c : code)
    {
        if (std::isalnum(c) == 0)
        {
            return false;
        }
    }
    return true;
}

HttpResponsePtr jsonResponse(const Json::Value &body, HttpStatusCode status)
{
    auto resp = HttpResponse::newHttpJsonResponse(body);
    resp->setStatusCode(status);
    return resp;
}

HttpResponsePtr errorResponse(const std::string &message, HttpStatusCode status)
{
    Json::Value body;
    body["error"] = message;
    return jsonResponse(body, status);
}

}  // namespace

int main()
{
    app().registerHandler(
        "/health",
        [](const HttpRequestPtr &,
           std::function<void(const HttpResponsePtr &)> &&callback) {
            Json::Value body;
            body["status"] = "ok";
            callback(jsonResponse(body, k200OK));
        },
        {Get});

    app().registerHandler(
        "/",
        [](const HttpRequestPtr &,
           std::function<void(const HttpResponsePtr &)> &&callback) {
            Json::Value body;
            body["service"] = "pastebin-cpp";
            body["docs"] = "none";
            callback(jsonResponse(body, k200OK));
        },
        {Get});

    app().registerHandler(
        "/paste",
        [](const HttpRequestPtr &req,
           std::function<void(const HttpResponsePtr &)> &&callback) {
            const auto &json = req->getJsonObject();
            if (json == nullptr || !(*json)["url"].isString() ||
                (*json)["url"].asString().empty())
            {
                callback(errorResponse("url is required", k400BadRequest));
                return;
            }

            const std::string url = (*json)["url"].asString();
            if (!isHttpUrl(url))
            {
                callback(errorResponse("url must be a valid http(s) URL",
                                       k400BadRequest));
                return;
            }

            std::string code;
            {
                std::lock_guard<std::mutex> lock(g_mutex);
                do
                {
                    code = generateCode();
                } while (g_pastes.find(code) != g_pastes.end());
                g_pastes.emplace(code, url);
            }

            Json::Value body;
            body["code"] = code;
            body["short_url"] = "/" + code;
            body["long_url"] = url;
            callback(jsonResponse(body, k201Created));
        },
        {Post});

    app().registerHandler(
        "/{1}",
        [](const HttpRequestPtr &,
           std::function<void(const HttpResponsePtr &)> &&callback,
           std::string code) {
            if (!isCodeShape(code))
            {
                callback(errorResponse("not found", k404NotFound));
                return;
            }

            std::string url;
            {
                std::lock_guard<std::mutex> lock(g_mutex);
                const auto it = g_pastes.find(code);
                if (it == g_pastes.end())
                {
                    callback(errorResponse("not found", k404NotFound));
                    return;
                }
                url = it->second;
            }

            // 302 Found by default, empty body.
            callback(HttpResponse::newRedirectionResponse(url));
        },
        {Get});

    app().addListener("0.0.0.0", 8080);
    app().run();
    return 0;
}
