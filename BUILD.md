# Build Guide

See [USAGE.md](USAGE.md) for installation and usage instructions.

## 1. Run the application from code

```sh
mvn compile exec:java -Dexec.mainClass=org.bidib.switchboard.demoapp.SwitchboardApp -pl switchboard-demo-app
```

Or build and run the executable JAR:

```sh
mvn clean package -DskipTests
java -jar switchboard-demo-app/target/switchboard-demo-app-1.0-SNAPSHOT.jar
```

## 2. Build the native Windows installer

The `switchboard-demo-wix-installer` module creates an MSI installer using WiX Toolset 6, Launch4j, and a bundled JRE.

```sh
mvn clean package -DskipTests -pl switchboard-demo-wix-installer -am
```

The output MSI is located at:

```
switchboard-demo-wix-installer/target/Release/x64/de-DE/Switchboard-Demo-1.0-SNAPSHOT-100-x64.msi
```

**Prerequisites:**
- Windows (WiX only builds on Windows)
- .NET 6+ runtime (required by WiX 6)
- Java 21+ and Maven 3.9+

The build will automatically:
1. Build `switchboard-demo-app` (fat JAR)
2. Create a Launch4j `.exe` wrapper
3. Bundle a JRE via `jlink`
4. Compile WiX sources into an MSI

## 3. Install SDKMAN and toolchain

[SDKMAN](https://sdkman.io) manages parallel versions of Java, Maven, and other JVM tools.

### Install Git for Windows

SDKMAN requires a Bash shell. Install Git for Windows (includes Git Bash):

```powershell
winget install Git.Git
```

After installation, open **Git Bash** for the steps below.

### Install SDKMAN (Windows — requires Git Bash or WSL)

```sh
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
```

On Windows native (PowerShell), use the [SDKMAN for Windows](https://github.com/nicorinu/sdkmanw) wrapper or install via Git Bash.

### Install Java and Maven

```sh
sdk install java 25.0.3-librca
sdk install maven 3.9.16
```

### Verify

```sh
java --version
# openjdk 25.0.3 2026-04-21 LTS (BellSoft Liberica)

mvn --version
# Apache Maven 3.9.16
```

### Set as default

```sh
sdk default java 25.0.3-librca
sdk default maven 3.9.16
```

The SDKs are installed under `~/.sdkman/candidates/java/current` and `~/.sdkman/candidates/maven/current`.
