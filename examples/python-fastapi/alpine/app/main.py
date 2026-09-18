import re
import secrets
import string

from fastapi import FastAPI
from fastapi.responses import JSONResponse, Response
from pydantic import BaseModel

app = FastAPI(title="pastebin-python")

_pastes: dict[str, str] = {}

_ALPHABET = string.ascii_letters + string.digits
_CODE_RE = re.compile(r"^[A-Za-z0-9]{8}$")


class PasteRequest(BaseModel):
    url: str | None = None


def _new_code() -> str:
    while True:
        code = "".join(secrets.choice(_ALPHABET) for _ in range(8))
        if code not in _pastes:
            return code


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/")
def root() -> dict[str, str]:
    return {"service": "pastebin-python", "docs": "/docs"}


@app.post("/paste", status_code=201)
def create_paste(payload: PasteRequest) -> JSONResponse:
    url = (payload.url or "").strip()
    if not url:
        return JSONResponse(status_code=400, content={"error": "url is required"})
    if not (url.startswith("http://") or url.startswith("https://")):
        return JSONResponse(
            status_code=400,
            content={"error": "url must be a valid http(s) URL"},
        )
    code = _new_code()
    _pastes[code] = url
    return JSONResponse(
        status_code=201,
        content={"code": code, "short_url": f"/{code}", "long_url": url},
    )


@app.get("/{code}")
def resolve_paste(code: str) -> Response:
    if not _CODE_RE.match(code):
        return JSONResponse(status_code=404, content={"error": "not found"})
    long_url = _pastes.get(code)
    if long_url is None:
        return JSONResponse(status_code=404, content={"error": "not found"})
    return Response(status_code=302, headers={"Location": long_url})
