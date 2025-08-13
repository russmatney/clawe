{ pkgs ? import <nixpkgs> {} }:

with pkgs;
mkShell {
  buildInputs = [
    hello

    babashka
    clojure
    clojure-lsp # should instead use the one from emacs
    clj-kondo

    jdk23
    libnotify
  ];
  shellHooks = ''
    hello
  '';
}
