# syntax=docker/dockerfile:1

# ---- Build stage: full .NET SDK + sources ----
FROM mcr.microsoft.com/dotnet/sdk:8.0-alpine AS build
WORKDIR /src
COPY PastebinApi.csproj ./
RUN dotnet restore ./PastebinApi.csproj
COPY . .
RUN dotnet publish PastebinApi.csproj -c Release -o /app/publish /p:UseAppHost=false

# ---- Runtime stage: ASP.NET runtime + published output only ----
FROM mcr.microsoft.com/dotnet/aspnet:8.0-alpine AS runtime
ENV DOTNET_RUNNING_IN_CONTAINER=true \
    ASPNETCORE_URLS=http://+:8080 \
    ASPNETCORE_ENVIRONMENT=Production
# The base image already ships an "app" user/group (uid/gid 1654); remove it so we
# can recreate "app" at uid/gid 1000 as required. icu-libs keeps globalization working.
RUN apk add --no-cache icu-libs \
    && (deluser app 2>/dev/null || true) \
    && (delgroup app 2>/dev/null || true) \
    && addgroup -S -g 1000 app \
    && adduser -S -G app -u 1000 app
WORKDIR /app
COPY --from=build /app/publish .
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=5 \
    CMD wget -qO- http://127.0.0.1:8080/health || exit 1
ENTRYPOINT ["dotnet", "PastebinApi.dll"]
