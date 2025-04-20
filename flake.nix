{
  description = "NixOS Flake with multiple JVM versions";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  inputs.flake-utils.url = "github:numtide/flake-utils";

  outputs = { self, nixpkgs, flake-utils }: 
    flake-utils.lib.eachDefaultSystem (system: 
      let
        pkgs = import nixpkgs { inherit system; };
        runtimeLibs = with pkgs; [
          # lwjgl
          glfw
          libpulseaudio
          libGL
          openal
          stdenv.cc.cc.lib

          vulkan-loader # VulkanMod's lwjgl

          udev # oshi

          xorg.libX11
          xorg.libXext
          xorg.libXcursor
          xorg.libXrandr
          xorg.libXxf86vm
        ];
      in {
        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            openjdk8
            openjdk17
            openjdk21
            jetbrains.jdk-no-jcef-17
          ];
          shellHook = ''
            export TMPDIR="/tmp/"
            export JAVA_HOME=${pkgs.jetbrains.jdk-no-jcef-17}
            export LD_LIBRARY_PATH=${nixpkgs.lib.makeLibraryPath runtimeLibs}
          '';
        };
      });
}
