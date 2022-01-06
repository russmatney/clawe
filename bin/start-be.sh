#!/usr/bin/env zsh
set -euo pipefail

clj -M:dev:doctor-deps:doctor-server
