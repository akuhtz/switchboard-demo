# Build-Anleitung

Siehe [USAGE-de.md](USAGE-de.md) für Installations- und Bedienungsanleitung.

## 1. Anwendung aus dem Quellcode starten

```sh
mvn compile exec:java -Dexec.mainClass=org.bidib.switchboard.demoapp.SwitchboardApp -pl switchboard-demo-app
```

Oder als ausführbares JAR bauen und starten:

```sh
mvn clean package -DskipTests
java -jar switchboard-demo-app/target/switchboard-demo-app-1.0-SNAPSHOT.jar
```

## 2. Nativen Windows-Installer bauen

Das Modul `switchboard-demo-wix-installer` erstellt einen MSI-Installer mit WiX Toolset 6, Launch4j und einer gebündelten JRE.

```sh
mvn clean package -DskipTests -pl switchboard-demo-wix-installer -am
```

Die erzeugte MSI-Datei befindet sich unter:

```
switchboard-demo-wix-installer/target/Release/x64/de-DE/Switchboard-Demo-1.0-SNAPSHOT-100-x64.msi
```

**Voraussetzungen:**
- Windows (WiX baut nur unter Windows)
- .NET 6+ Runtime (erforderlich für WiX 6)
- Java 21+ und Maven 3.9+

Der Build führt automatisch folgende Schritte aus:
1. `switchboard-demo-app` bauen (Fat JAR)
2. Launch4j `.exe`-Wrapper erstellen
3. JRE über `jlink` bündeln
4. WiX-Quellen zu einer MSI kompilieren

## 3. SDKMAN und Toolchain installieren

[SDKMAN](https://sdkman.io) verwaltet parallele Versionen von Java, Maven und anderen JVM-Tools.

### Git für Windows installieren

SDKMAN benötigt eine Bash-Shell. Git für Windows installieren (enthält Git Bash):

```powershell
winget install Git.Git
```

Nach der Installation **Git Bash** für die folgenden Schritte öffnen.

### SDKMAN installieren (Windows — erfordert Git Bash oder WSL)

```sh
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
```

Unter nativem Windows (PowerShell) den [SDKMAN for Windows](https://github.com/nicorinu/sdkmanw)-Wrapper verwenden oder über Git Bash installieren.

### Java und Maven installieren

```sh
sdk install java 25.0.3-librca
sdk install maven 3.9.16
```

### Überprüfen

```sh
java --version
# openjdk 25.0.3 2026-04-21 LTS (BellSoft Liberica)

mvn --version
# Apache Maven 3.9.16
```

### Als Standard setzen

```sh
sdk default java 25.0.3-librca
sdk default maven 3.9.16
```

Die SDKs werden unter `~/.sdkman/candidates/java/current` und `~/.sdkman/candidates/maven/current` installiert.
