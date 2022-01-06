#!/usr/bin/env zsh
set -euo pipefail

cd ~/russmatney/clover
yarn run dev http://localhost:3333/screenshots doctor-screenshots
