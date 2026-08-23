# Benutzerhandbuch

## 1. Anwendung installieren

Den aktuellen Windows-Installer (MSI) herunterladen:

[![Installer für Windows](https://img.shields.io/badge/Download-Windows%20Installer-blue)](https://www.fichtelbahn.de/files/wizard-fw/switchboard-demo/getLatestLink.php?type=msi&version=1.0)

Der Installer enthält eine JRE — eine separate Java-Installation ist nicht erforderlich. Nach der Installation kann **Switchboard Demo** über das Startmenü oder die Desktop-Verknüpfung gestartet werden.

## 2. Auf ein leeres Raster zurücksetzen

- Den **Schraubenschlüssel** in der Symbolleiste anklicken oder **Strg+E** drücken, um den Bearbeitungsmodus zu aktivieren.
- Im Menü: **Datei → Laden...** und ein leeres JSON-Layout auswählen (oder alle Kacheln einzeln über **Löschen** im Kontextmenü entfernen).
- Alternativ `switchboard-demo-app/settings.json` löschen, dann die zuletzt geladene Layout-Datei entfernen — die Anwendung startet mit dem eingebauten Standard-Layout. Kacheln können dann einzeln gelöscht werden.

## 3. Kacheln hinzufügen

- Rechtsklick auf eine beliebige Zelle → einen Kacheltyp aus der Liste auswählen (z.B. `P (STRAIGHT)`, `TR (TURNOUT_RIGHT)`, `TL (TURNOUT_LEFT)`, `SM3 (SIGNAL_M3)`, `SV (SIGNAL_V)`, `SM (SIGNAL_COMBINED)`).
- Die Kachel erscheint sofort.
- **Drehen**: Kachel mit Linksklick auswählen (cyan Rahmen), dann **Strg+R** drücken, um 90° zu drehen.

Folgende Kacheltypen sind verfügbar:

| Typ | Präfix | Beschreibung | Drehungen |
|-----|--------|--------------|-----------|
| STRAIGHT       | P  | Gerades Gleis                | 0 / 90     |
| CURVE_LEFT     | CL | 90°-Kurve nach links         | 0 / 90 / 180 / 270 |
| CURVE_RIGHT    | CR | 90°-Kurve nach rechts        | 0 / 90 / 180 / 270 |
| DIAGONAL       | DG | Diagonales Gleis (↗)         | 0 / 90 / 180 / 270 |
| DIAGONAL_TURNOUT_RIGHT | DTR | Diagonalweiche (2 Stellungen): gerade ↗ oder nach rechts abzweigend | 0 / 90 / 180 / 270 |
| DIAGONAL_TURNOUT_LEFT | DTL | Diagonalweiche (2 Stellungen): gerade ↘ oder nach links abzweigend | 0 / 90 / 180 / 270 |
| TURNOUT_LEFT   | TL | Linksweiche (2 Stellungen)   | 0 / 90 / 180 / 270 |
| TURNOUT_RIGHT  | TR | Rechtsweiche (2 Stellungen)  | 0 / 90 / 180 / 270 |
| TURNOUT_3WAY   | T3 | Dreiwegeweiche (3 Stellungen)| 0 / 90 / 180 / 270 |
| SIGNAL_M3       | SM3 | 3-begriffiges Signal (rot/grün/orange) | 0 / 90 / 180 / 270 |
| SIGNAL_V       | SV | Vorsignal (orange/grün/orange+grün/Stellung 3) | 0 / 90 / 180 / 270 |
| SIGNAL_COMBINED | SM | Kombinationssignal (Hauptsignal + Vorsignalplatte auf einem Mast) | 0 / 90 / 180 / 270 |
| BUMPER         | BS | Prellbock (Gleisende)                | 0 / 90 / 180 / 270 |
| BLOCK_MARKER   | BM | Blockmarkierung (gerades Gleis, zeigt Blockname) | 0 / 90 |

## 4. Kacheln auswählen und verschieben

Im **Bearbeitungsmodus** (Strg+E) können Kacheln ausgewählt und verschoben werden:

- **Einzelne Kachel**: Linksklick auf eine Kachel zur Auswahl (cyan Rahmen).
- **Bereichsauswahl**: Klicken und Ziehen, um ein Auswahlrechteck über mehrere Kacheln zu zeichnen. Alle Kacheln im Rechteck werden ausgewählt (transluzenter cyan-Hervorhebung).
- **Verschieben mit Pfeiltasten**: **Aufwärts** / **Abwärts** / **Links** / **Rechts** drücken, um alle ausgewachten Kacheln um eine Zelle zu verschieben. Drehung und alle Kachel-Eigenschaften werden beibehaltet. Fahrstraßen, die durch verschobene Kacheln verlaufen, werden entsprechend aktualisiert.
- **Rückgängig**: **Strg+Z** drücken, um die letzte Verschiebung rückgängig zu machen.
- **Auswahl aufheben**: **Esc** drücken, woanders klicken, oder Rechtsklick → **Auswahl aufheben**.

## 5. Elemente klicken, um Stellungen zu wechseln

Im **Normalmodus** (Strg+E zum Umschalten):
- Auf eine Weiche oder ein Signal klicken, um die Stellung zu wechseln (gerade ↔ abzweigend, rot ↔ grün, orange ↔ grün ↔ orange+grün ↔ Stellung 3 beim Vorsignal usw.).
- Ein Klick auf ein **Hauptsignal** schaltet auch jedes **verknüpfte Vorsignal** auf die passende Vorankündigung (siehe [Abschnitt 9](#9-vorsignal-mit-hauptsignal-verkn%C3%BCpfen)). Ein verknüpftes Vorsignal selbst lässt sich nicht anklicken, um seine Stellung zu ändern.
- Ein **Kombinationssignal** verhält sich wie ein Hauptsignal: Durch Klicken wird die Stellung seines eigenen Hauptsignalkopfes gewechselt (rot/grün/orange). Seine Vorsignalplatte wird über das verknüpfte Hauptsignal gesteuert und lässt sich nicht anklicken.
- Weichen werden automatisch auf die korrekte Stellung gesetzt, wenn eine Fahrstraße gefunden wird.

## 6. Fahrstraße erstellen

- Sicherstellen, dass der **Normalmodus** aktiv ist (Strg+E zum Deaktivieren des Bearbeitungsmodus).
- **Strg+Klick** auf eine Startkachel — eine grüne Markierung erscheint.
- **Strg+Klick** auf eine Zielkachel — der kürzeste Weg wird gefunden und als blaue Polylinie mit grüner (Start) und blauer (Ziel) Markierung gezeichnet.
- Weichen entlang der Fahrstraße werden automatisch auf die korrekte Stellung gesetzt.
- Die Fahrstraßensuche verwendet BFS mit physischer Port-Konnektivitätsprüfung. Fahrstraßen berücksichtigen die Weichenrichtung (kein Rückwärts-Durchfahren am Herzstück), Richtungsmarkierungen und vermeiden bereits durch andere Fahrstraßen reservierte Kacheln.
- **Alternative Fahrstraßen**: Bei der Erstellung einer Fahrstraße findet BFS alternative Wege, indem jede Kante des Primärpfads blockiert wird. Ein weißes **"+"**-Symbol erscheint neben den Start- und Zielmarkierungen, wenn Alternativen verfügbar sind. Rechtsklick auf eine Fahrstraßenkachel zeigt sie im Kontextmenü:
  - **Alternative 1 / Alternative 2 / ...** — Vorschau der Alternative als gestrichelte Linie in eigener Farbe aus einer 16-Farben-Palette, über der Hauptfahrstraße. Jeder Menüeintrag zeigt ein farbiges Kreissymbol passend zur Fahrstraßenfarbe. Die Hauptfahrstraße bleibt sichtbar.
  - **Primärfahrstraße verwenden** — Alternativen verwerfen und die originale blaue Fahrstraße anzeigen.
  - **Ausgewählte Alternative verwenden** — die vorgeschaute Alternative zur Primärfahrstraße machen.
- **Erschöpfende Fahrstraßensuche**: Aktivieren unter **Datei → Einstellungen → Erschöpfende Fahrstraßensuche**. Wenn aktiv, blockiert BFS auch Kanten gefundener Alternativen (k-kürzeste-Wege-Iteration) und findet so mehr verschiedene Fahrstraßen. Die Einstellung wird in `switchboard-demo-app/settings.json` gespeichert.

### Fahrstraßen verwalten

- **Einzelne Fahrstraße löschen**: Rechtsklick auf eine Kachel der Fahrstraße → **Fahrstraße löschen ({id})**.
- **Alle Fahrstraßen löschen**: **Clear selection** im Kontextmenü (nur im Bearbeitungsmodus) oder programmatisch über das Modell.
- Mehrere nicht-überlappende Fahrstraßen können gleichzeitig existieren — BFS findet einen Weg um bestehende Fahrstraßenkacheln.

## 7. Kachelrichtung

Gerade und diagonale Kacheln können eine **Richtungsbeschränkung** haben (FORWARD, BACKWARD oder BOTH).

- Im **Bearbeitungsmodus** Rechtsklick auf eine gerade oder diagonale Kachel → **Richtung**-Untermenü → Forward / Backward / Both auswählen.
- Eine kleine hellgraue Dreiecksmarkierung erscheint in der Kachelmitte und zeigt in die erlaubte Richtung.
- Die Fahrstraßensuche berücksichtigt die Richtung: BFS wird eine Kachel nicht gegen ihre Richtung durchfahren.
- Standard ist **Both** (keine Beschränkung) — abwärtskompatibel mit bestehenden Layouts.

## 8. Signalseite

Signalkacheln können ihr Gehäuse oberhalb (Schweizer Norm, `_left`) oder unterhalb (deutsche Norm, `_right`) des Gleises anzeigen. Dies gilt für alle Signaltypen (SIGNAL_M3, SIGNAL_V, SIGNAL_COMBINED).

- Der globale Standard wird unter **Datei → Einstellungen → Signalseite Links / Signalseite Rechts** festgelegt.
- Kachelspezifische Einstellung: im **Bearbeitungsmodus** Rechtsklick auf eine Signalkachel → **Signalseite**-Untermenü → Links / Rechts / Standard auswählen.
- Bei Änderung der Signalseite wird das Kachelbild sofort aktualisiert.
- Der **Kachel-Info**-Dialog (Linksklick auf ein Signal im Normalmodus) zeigt die aufgelöste Signalseite an.

## 9. Vorsignal mit Hauptsignal verknüpfen

Ein Vorsignal (SIGNAL_V) kann mit dem Hauptsignal (SIGNAL_M3 oder SIGNAL_COMBINED) verknüpft werden, das es ankündigt. Die Vorsignalplatte eines Kombinationssignals (SIGNAL_COMBINED) verwendet dieselbe Verknüpfung. Nach der Verknüpfung gilt:

- **Manuelle Spiegelung**: Im Normalmodus schaltet ein Klick auf das Hauptsignal auch jedes verknüpfte Vorsignal auf die passende Vorankündigung: rot → orange „Halt erwarten", grün → grün „Frei erwarten", bei SIGNAL_M3 zusätzlich orange → orange+grün „Langsamfahrt erwarten". Dies funktioniert auch außerhalb der Simulation.
- Ein Klick auf ein **verknüpftes Vorsignal** selbst ändert dessen Stellung **nicht** — seine Stellung wird über das verknüpfte Hauptsignal gesteuert.
- **Zuordnung**: Im Bearbeitungsmodus Rechtsklick auf das Vorsignal → **Hauptsignal zuweisen** → ein Hauptsignal aus der Liste wählen (oder **Keins**). Das nächste Hauptsignal in Fahrtrichtung entlang des verbundenen Gleises wird vorausgewählt und mit „(auto)" markiert — dabei folgt die Suche Kurven (einschließlich diagonaler Bogenecken) sowie Weichen entsprechend ihrer aktuellen Stellung und überbrückt keine Lücken.
- **Automatische Zuweisung beim Drehen**: Das Vorsignal auswählen und **Strg+R** drücken. Das Hauptsignal vor ihm in der neuen Fahrtrichtung entlang des Gleises (Kurven und Weichenstellung folgend) wird automatisch zugewiesen. War das Vorsignal bereits mit einem anderen Hauptsignal verknüpft, wird die alte Verknüpfung ersetzt und das Vorsignal auf die aktuelle Stellung des neuen Hauptsignals umgeschaltet. Beide Änderungen werden protokolliert.
- **Entfernen**: Beim Löschen eines Hauptsignals mit verknüpften Vorsignalen wird gefragt, ob die Vorsignale mit entfernt (**Verknüpfte entfernen**), behalten (**Behalten** — die Verknüpfung wird gelöst) oder ob abgebrochen (**Abbrechen**) werden soll.
- Die Verknüpfung wird mit dem Layout gespeichert und beim Laden wiederhergestellt.
- Der **Kachel-Info**-Dialog zeigt die Verknüpfung an (Hauptsignal / Vorsignale) und bei Kombinationssignalen die aktuelle Stellung der Vorsignalplatte.

## 10. Blöcke definieren

Ein **Block** ist ein verbundener Pfad aus Kacheln, der einen Gleisabschnitt bildet. Blöcke sind
nützlich, um Belegungsabschnitte zu modellieren.

- Im **Bearbeitungsmodus** (Strg+E) Rechtsklick auf die erste Kachel → **Block → Blockanfang festlegen**.
- Rechtsklick auf die letzte Kachel → **Block → Blockende festlegen**. Der verbundene Pfad zwischen
  den beiden Kacheln wird automatisch gefunden und ein Block erstellt.
- **Keine Weichen**: Blöcke führen nie durch Weichen-Kacheln (TL/TR/T3). Wenn kein weichenfreier
  verbundener Pfad existiert, wird kein Block erstellt.
- **Keine Überlappungen**: Jede Kachel gehört zu höchstens einem Block. Kacheln, die bereits einem
  anderen Block zugeordnet sind, werden bei der Pfadsuche vermieden.
- Jeder Block erhält eine **eindeutige ID** und einen Standardnamen `blk001`, `blk002`, ...
  (mit führenden Nullen).
- Zum Umbenennen Rechtsklick auf eine Kachel des Blocks → **Block → Block umbenennen...** und im
  Dialog einen neuen Namen eingeben.
- Zum Entfernen eines Blocks Rechtsklick auf eine Kachel des Blocks → **Block → Block entfernen**.
- Blöcke werden als 2 px breite gelbe Linie unterhalb des Gleises entlang des Pfads gezeichnet,
  mit einem kurzen vertikalen Strich an der Außenkante der Start- und Endkachel. Der ausstehende
  Blockanfang wird als   orangefarbene quadratische Markierung angezeigt.
- Blöcke werden mit dem Layout gespeichert und beim Laden wiederhergestellt.

## 11. Blockmarkierungen

Blockmarkierungen (`BLOCK_MARKER` / `BM`) sind gerade Gleiskacheln, die den Blocknamen als zentrierte Textbeschriftung anzeigen. Sie helfen bei der visuellen Identifizierung von Blockgrenzen auf dem Schaltpult.

- Blockmarkierung platzieren: Rechtsklick auf eine Zelle im Bearbeitungsmodus → **BM (BLOCK_MARKER)**.
- Die Markierung zeigt den Blocknamen in Gelb (`(255,220,80)`) an, wenn der Block nicht belegt ist, oder in Rot wenn belegt.
- Blockmarkierungen können wie gerade Gleiskacheln gedreht werden (0° oder 90°).
- Eine Blockmarkierung kann einen **Zug** zugewiesen bekommen per Drag-and-Drop (siehe [Abschnitt 11](#11-züge-und-draganddrop)). Wenn ein Zug zugewiesen ist, zeigt die Markierung den Zugnamen statt des Blocknamens an.

## 12. Züge und Drag-and-Drop

Die Anwendung zeigt eine Zugliste links im Fenster an, die alle definierten Züge anzeigt.

### Züge hinzufügen

Züge werden in einer `trains.json`-Datei definiert, die vom aktuellen Layout referenziert wird. Wenn Sie ein Layout laden, das eine Zugsdatei referenziert, wird die Zugliste automatisch gefüllt. Um einen neuen Zug hinzuzufügen, bearbeiten Sie die `train.json`-Datei und laden Sie das Layout neu.

Jeder Zug hat:
- **id** (erforderlich): eindeutige Kennung, z.B. `"train-1"`
- **name** (erforderlich): Anzeigename, z.B. `"ICE 701"`
- **address** (optional): DCC/NMRA-Adresse als Ganzzahl
- **image** (optional): Pfad zu einer Bilddatei

### Züge Blöcken zuweisen

1. Im **Normalmodus** einen Zug aus der Zugliste ziehen und auf eine **Blockmarkierung** loslassen.
2. Die Blockmarkierung zeigt dann den **Zugnamen** statt des Blocknamens an.
3. Jeder Zug kann nur einem Block zugewiesen sein — das Ablegen eines Zugs auf einem anderen Block entfernt ihn automatisch aus dem vorherigen Block.
4. Um eine Zugzuordnung aufzuheben, Rechtsklick auf die Blockmarkierung und **Zug entfernen** wählen. Diese Option ist sowohl im Bearbeitungsmodus als auch im Normalmodus für Blockmarkierungen mit einem zugewiesenen Zug verfügbar.

### Drag-and-Drop-Details

- Der **Drop-Cursor** (Pfeil + Pluszeichen) erscheint nur beim Überfahren einer Blockmarkierungskachel, nicht über regulären Gleiskacheln.
- Das Ziehen eines Zugs auf eine nicht-Blockmarkierungskachel hat keine Auswirkung.

## 13. Belegung simulieren

Nach dem Erstellen einer Fahrstraße kann ein Zug entlang der Strecke animiert werden:

- Rechtsklick auf den **grünen Startkreis** einer Fahrstraße → **Belegung simulieren ({id})**.
- Die Simulation erzeugt Belegungsmarkierungen auf jeder Kachel der Fahrstraße und schiebt den **OCCUPIED**-Zustand vom Start zum Ende, eine Kachel pro Schritt (200ms pro Schritt).
- Weichen entlang der Fahrstraße werden automatisch auf die korrekte Position für den simulierten Weg gesetzt.
- **Signalhalt**: Wenn ein Zug ein Hauptsignal (SIGNAL_M3 oder SIGNAL_COMBINED) mit Stellung 0 (rot) erreicht, hält er an und wartet. Signale blockieren nur Züge, die sich von vorne nähern (die Richtung, in die das Signal basierend auf seiner Rotation zeigt). Züge, die sich einem Signal von hinten nähern, ignorieren es.
- **Vorsignale (SIGNAL_V)**: Das Vorsignal hält den Zug nie an. Es spiegelt die Stellung des nächsten Hauptsignals im Pfad wider und kündigt so die kommende Stellung an: orange „Halt erwarten", grün „Frei erwarten", orange+grün „Langsamfahrt erwarten". Ein mit einem Hauptsignal verknüpftes Vorsignal (siehe [Abschnitt 9](#9-vorsignal-mit-hauptsignal-verkn%C3%BCpfen)) spiegelt dessen Stellung auch beim manuellen Klicken außerhalb der Simulation.
- **Kombinationssignale**: Der Hauptsignalkopf hält den Zug wie ein Hauptsignal an. Während der Simulation spiegelt die Vorsignalplatte des Kombinationssignals das nächste Hauptsignal im Pfad (`syncCombinedPlate`); außerhalb einer Simulation spiegelt sie das verknüpfte Hauptsignal.
- **Automatischer Signalwechsel**: Aktivieren über **Bearbeiten → Automatischer Signalwechsel**. Wenn aktiv, schaltet ein Hauptsignal, das einen Zug blockiert, nach 2 Sekunden automatisch auf Stellung 1 (grün) um, sodass der Zug weiterfahren kann. Das Umschalten dieser Option wirkt sich sofort auf alle laufenden Simulationen aus.
- **Mehrere Simulationen**: Jede Fahrstraße kann ihre eigene unabhängige Simulation gleichzeitig ausführen.
- **Simulation stoppen**: Rechtsklick auf die Startkachel einer laufenden Simulation → **Simulation stoppen ({id})**, um sie mitten in der Strecke zu stoppen.
- **Belegung simulieren** ist deaktiviert, solange die Simulation dieser Fahrstraße bereits läuft.

Simulation zurücksetzen:

- Rechtsklick auf eine Kachel einer Fahrstraße mit OCCUPIED-Kacheln → **Simulierte Belegung löschen ({id})**.
- Setzt alle Belegungszustände entlang der Fahrstraße auf FREE zurück.
- **Simulierte Belegung löschen** ist deaktiviert, solange eine Simulation läuft.

## 14. Sprache / Internationalisierung

Die Anwendung unterstützt die Sprachen **Englisch** und **Deutsch**. Die UI-Sprache wird beim Start ermittelt: Falls eine Sprache unter **Datei → Einstellungen → Sprache** gespeichert ist, wird diese verwendet, sonst die Systemsprache. Die gewählte Sprache wird gespeichert und beim nächsten Start wieder angewendet.

- **Kontextmenü**-Einträge (Info, Signalseite, Hauptsignal zuweisen, Löschen, Richtung, Fahrstraßenaktionen) werden lokalisiert angezeigt.
- Der **Kachel-Info**-Dialog verwendet übersetzte Bezeichnungen.
- **Hauptmenü** und **Symbolleiste** werden lokalisiert angezeigt (Datei, Bearbeiten, Einstellungen usw.).
- Falls Ihre Systemsprache Deutsch ist, wechselt die UI automatisch zu deutschen Bezeichnungen. Sie können die Sprache auch über `-Duser.language=en` oder `-Duser.language=de` in der Java-Befehlszeile erzwingen.
- Zum Wechseln der Sprache während des Betriebs: **Datei → Einstellungen → Sprache → Englisch / Deutsch**. Die Auswahl wird gespeichert und sofort angewendet (Menüs, Tooltips und Kontextmenüs aktualisieren sich).

Die Übersetzungen befinden sich in `i18n/messages.properties` (Komponente) und `i18n/app-messages.properties` (Demo-Anwendung), jeweils mit einer `_de`-Variante.

## 15. Speichern & Laden

- **Strg+S** — in die aktuelle Datei speichern (oder Speichern-Dialog öffnen, falls keine vorhanden).
- **Strg+L** — ein zuvor gespeichertes `.json`-Layout laden.
- Beim Start merkt sich die Anwendung die zuletzt geladene Datei und stellt sie automatisch wieder her.
- Einstellungen werden in `~/switchboard-demo-1/settings.json` gespeichert.

## 16. Protokollierung

Die Anwendung schreibt Log-Ausgaben sowohl auf die Konsole als auch in eine Protokolldatei:

- **Windows**: `%USERPROFILE%\Documents\switchboard-demo\switchboard-demo-app.log`
- **Andere Betriebssysteme**: `<java.io.tmpdir>/switchboard-demo-app.log`

Das Protokollverzeichnis wird bei Bedarf automatisch erstellt. Um einen anderen
Speicherort zu verwenden, starten Sie die Anwendung mit `-Dswitchboard.logfile=/pfad/zur/logdatei`.
Die Protokolldatei hilft bei der Fehlersuche bei Start- oder Layout-Ladeproblemen.
