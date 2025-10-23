{
  description = "A very basic flake";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";
  };

  outputs = {
    self,
    nixpkgs,
  }: let
    pkgs = nixpkgs.legacyPackages.x86_64-linux;
  in {
    devShell.x86_64-linux = with pkgs;
      mkShell {
        buildInputs = [
          gradle
          jdk17
        ];
        packages = with pkgs; [
          (python3.withPackages (python-pkgs:
            with python-pkgs; [
              matplotlib
              numpy
              seaborn
              ipykernel
            ]))
        ];
        NIX_LD_LIBRARY_PATH = with pkgs;
          lib.makeLibraryPath [
            stdenv.cc.cc
            openssl
          ];
        NIX_LD = pkgs.lib.fileContents "${pkgs.stdenv.cc}/nix-support/dynamic-linker";
      };
  };
}
