# Benutzerhandbuch

## 1. Anwendung installieren

Den aktuellen Windows-Installer (MSI) herunterladen:

[![Installer für Windows](https://img.shields.io/badge/Download-Windows%20Installer-blue)](https://www.fichtelbahn.de/files/wizard-fw/switchboard-demo/getLatestLink.php?type=msi&version=1.0)

Der Installer enthält eine JRE — eine separate Java-Installation ist nicht erforderlich. Nach der Installation kann **Switchboard Demo** über das Startmenü oder die Desktop-Verknüpfung gestartet werden.

## 2. Auf ein leeres Raster zurücksetzen

- **Strg+E** drücken, um den Bearbeitungsmodus zu aktivieren.
- Im Menü: **File → Load...** und ein leeres JSON-Layout auswählen (oder alle Kacheln einzeln über **Clear** im Kontextmenü entfernen).
- Alternativ `switchboard-demo-app/settings.json` löschen, dann die zuletzt geladene Layout-Datei entfernen — die Anwendung startet mit dem eingebauten Standard-Layout. Kacheln können dann einzeln gelöscht werden.

## 3. Kacheln hinzufügen

- Rechtsklick auf eine beliebige Zelle → einen Kacheltyp aus der Liste auswählen (z.B. `P (STRAIGHT)`, `TR (TURNOUT_RIGHT)`, `TL (TURNOUT_LEFT)`, `S2 (SIGNAL_2)`).
- Die Kachel erscheint sofort.
- **Drehen**: Kachel mit Linksklick auswählen (cyan Rahmen), dann **Strg+R** drücken, um 90° zu drehen.

Folgende Kacheltypen sind verfügbar:

| Typ | Präfix | Beschreibung | Drehungen |
|-----|--------|--------------|-----------|
| STRAIGHT       | P  | Gerades Gleis                | 0 / 90     |
| CURVE_LEFT     | CL | 90°-Kurve nach links         | 0 / 90 / 180 / 270 |
| CURVE_RIGHT    | CR | 90°-Kurve nach rechts        | 0 / 90 / 180 / 270 |
| DIAGONAL       | DG | Diagonales Gleis (↗)         | 0 / 90 / 180 / 270 |
| TURNOUT_LEFT   | TL | Linksweiche (2 Stellungen)   | 0 / 90 / 180 / 270 |
| TURNOUT_RIGHT  | TR | Rechtsweiche (2 Stellungen)  | 0 / 90 / 180 / 270 |
| TURNOUT_3WAY   | T3 | Dreiwegeweiche (3 Stellungen)| 0 / 90 / 180 / 270 |
| SIGNAL_2       | S2 | 2-begriffiges Signal (rot/grün)  | 0 / 90 / 180 / 270 |
| SIGNAL_3       | S3 | 3-begriffiges Signal (rot/gelb/grün) | 0 / 90 / 180 / 270 |
| BUMPER         | BS | Prellbock (Gleisende)                | 0 / 90 / 180 / 270 |

## 4. Elemente klicken, um Stellungen zu wechseln

Im **Normalmodus** (Strg+E zum Umschalten):
- Auf eine Weiche oder ein Signal klicken, um die Stellung zu wechseln (gerade ↔ abzweigend, rot ↔ grün usw.).
- Weichen werden automatisch auf die korrekte Stellung gesetzt, wenn eine Fahrstraße gefunden wird.

## 5. Fahrstraße erstellen

- Sicherstellen, dass der **Normalmodus** aktiv ist (Strg+E zum Deaktivieren des Bearbeitungsmodus).
- **Strg+Klick** auf eine Startkachel — eine grüne Markierung erscheint.
- **Strg+Klick** auf eine Zielkachel — der kürzeste Weg wird gefunden und als blaue Polylinie mit grüner (Start) und blauer (Ziel) Markierung gezeichnet.
- Weichen entlang der Fahrstraße werden automatisch auf die korrekte Stellung gesetzt.
- Die Fahrstraßensuche verwendet BFS mit physischer Port-Konnektivitätsprüfung. Fahrstraßen berücksichtigen die Weichenrichtung (kein Rückwärts-Durchfahren am Herzstück), Richtungsmarkierungen und vermeiden bereits durch andere Fahrstraßen reservierte Kacheln.
- **Alternative Fahrstraßen**: Bei der Erstellung einer Fahrstraße findet BFS alternative Wege, indem jede Kante des Primärpfads blockiert wird. Ein weißes **"+"**-Symbol erscheint neben den Start- und Zielmarkierungen, wenn Alternativen verfügbar sind. Rechtsklick auf eine Fahrstraßenkachel zeigt sie im Kontextmenü:
  - **Alternative 1 / Alternative 2 / ...** — Vorschau der Alternative als gestrichelte Linie in eigener Farbe aus einer 16-Farben-Palette, über der Hauptfahrstraße. Jeder Menüeintrag zeigt ein farbiges Kreissymbol passend zur Fahrstraßenfarbe. Die Hauptfahrstraße bleibt sichtbar.
  - **Use primary route** — Alternativen verwerfen und die originale blaue Fahrstraße anzeigen.
  - **Use selected alternative** — die vorgeschaute Alternative zur Primärfahrstraße machen.
- **Exhaustive Route Search**: Aktivieren unter **File → Settings → Exhaustive Route Search**. Wenn aktiv, blockiert BFS auch Kanten gefundener Alternativen (k-kürzeste-Wege-Iteration) und findet so mehr verschiedene Fahrstraßen. Die Einstellung wird in `switchboard-demo-app/settings.json` gespeichert.

### Fahrstraßen verwalten

- **Einzelne Fahrstraße löschen**: Rechtsklick auf eine Kachel der Fahrstraße → **Clear route ({id})**.
- **Alle Fahrstraßen löschen**: **Clear selection** im Kontextmenü (nur im Bearbeitungsmodus) oder programmatisch über das Modell.
- Mehrere nicht-überlappende Fahrstraßen können gleichzeitig existieren — BFS findet einen Weg um bestehende Fahrstraßenkacheln.

## 6. Kachelrichtung

Gerade und diagonale Kacheln können eine **Richtungsbeschränkung** haben (FORWARD, BACKWARD oder BOTH).

- Im **Bearbeitungsmodus** Rechtsklick auf eine gerade oder diagonale Kachel → **Direction**-Untermenü → Forward / Backward / Both auswählen.
- Eine kleine hellgraue Dreiecksmarkierung erscheint in der Kachelmitte und zeigt in die erlaubte Richtung.
- Die Fahrstraßensuche berücksichtigt die Richtung: BFS wird eine Kachel nicht gegen ihre Richtung durchfahren.
- Standard ist **Both** (keine Beschränkung) — abwärtskompatibel mit bestehenden Layouts.

## 7. Belegung simulieren

Nach dem Erstellen einer Fahrstraße kann ein Zug entlang der Strecke animiert werden:

- Rechtsklick auf den **grünen Startkreis** einer Fahrstraße → **Simulate occupancy ({id})**.
- Die Simulation erzeugt Belegungsmarkierungen auf jeder Kachel der Fahrstraße und schiebt den **OCCUPIED**-Zustand vom Start zum Ende, eine Kachel pro Schritt (200ms pro Schritt).
- Weichen entlang der Fahrstraße werden automatisch auf die korrekte Position für den simulierten Weg gesetzt.
- **Signalhalt**: Wenn ein Zug eine Signalkachel mit Stellung 0 (rot) erreicht, hält er an und wartet. Signale blockieren nur Züge, die sich von vorne nähern (die Richtung, in die das Signal basierend auf seiner Rotation zeigt). Züge, die sich einem Signal von hinten nähern, ignorieren es.
- **Automatischer Signalwechsel**: Aktivieren über **Edit → Auto-change signal**. Wenn aktiv, schaltet ein rotes Signal, das einen Zug blockiert, nach 2 Sekunden automatisch auf grün um, sodass der Zug weiterfahren kann. Das Umschalten dieser Option wirkt sich sofort auf alle laufenden Simulationen aus.
- **Mehrere Simulationen**: Jede Fahrstraße kann ihre eigene unabhängige Simulation gleichzeitig ausführen.
- **Simulation stoppen**: Rechtsklick auf die Startkachel einer laufenden Simulation → **Stop simulation ({id})**, um sie mitten in der Strecke zu stoppen.
- **Simulate occupancy** ist deaktiviert, solange die Simulation dieser Fahrstraße bereits läuft.

Simulation zurücksetzen:

- Rechtsklick auf eine Kachel einer Fahrstraße mit OCCUPIED-Kacheln → **Clear simulated occupancy ({id})**.
- Setzt alle Belegungszustände entlang der Fahrstraße auf FREE zurück.
- **Clear simulated occupancy** ist deaktiviert, solange eine Simulation läuft.

## 8. Speichern & Laden

- **Strg+S** — in die aktuelle Datei speichern (oder Speichern-Dialog öffnen, falls keine vorhanden).
- **Strg+L** — ein zuvor gespeichertes `.json`-Layout laden.
- Beim Start merkt sich die Anwendung die zuletzt geladene Datei und stellt sie automatisch wieder her.
- Einstellungen werden in `~/switchboard-demo-1/settings.json` gespeichert.
