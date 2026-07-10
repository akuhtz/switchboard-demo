# Model Railway Switchboard – Component Specification

## Overview

A Java Swing-based switchboard component for controlling and visualising a model railway layout.
The component manages switches (points) and signals, responds to user interaction, and can be
extended for DCC hardware integration.

---

## Architecture

### Design Patterns

| Pattern       | Purpose                                                                 |
|---------------|-------------------------------------------------------------------------|
| **MVC**       | Separates layout state (Model), rendering (View), and user actions (Controller) |
| **Observer**  | Propagates model state changes to the UI automatically                  |
| **Command**   | Encapsulates actions (toggle switch, set signal) with undo/redo support |
| **State**     | Models per-element states (switch: straight/diverted, signal: red/green)|
| **Composite** | Composes track segments, switches, and signals into a unified panel     |

---

## Components

### `RailwayModel`
- Central model class holding the state of all railway elements.
- Uses `PropertyChangeSupport` (idiomatic Java Observer, replaces deprecated `java.util.Observable`).
- Manages:
  - **Switches** (`Map<String, SwitchState>`) — states: `STRAIGHT`, `DIVERTED`
  - **Signals** (`Map<String, SignalState>`) — states: `RED`, `GREEN`
- Fires `PropertyChangeEvent` on every state mutation.
- Methods:
  - `addSwitch(String id)`
  - `addSignal(String id)`
  - `toggleSwitch(String id)`
  - `setSignal(String id, SignalState state)`
  - `getSwitchState(String id)`
  - `getSignalState(String id)`
  - `addPropertyChangeListener(PropertyChangeListener l)`

---

### `SwitchboardPanel` (View)
- Extends `JPanel`, implements `PropertyChangeListener`.
- Registers as observer on the `RailwayModel`.
- Maintains a `Map<String, Rectangle> elementBounds` for element placement and hit-testing.
- **Rendering** (`paintComponent`):
  - Uses `Graphics2D` with antialiasing.
  - Switches rendered as rounded rectangles (cyan = straight, orange = diverted).
  - Signals rendered as filled ovals (green / red).
- **Interaction**:
  - `MouseListener` performs hit-testing against `elementBounds` on click.
  - Delegates toggle actions directly to the model.
- **Thread safety**:
  - All repaints triggered via `SwingUtilities.invokeLater` to ensure execution on the Event Dispatch Thread (EDT).
- Public API:
  - `registerElement(String id, Rectangle bounds)` — registers a visual element and its screen position.

---

### `Command` (Interface)
- `void execute()`
- `void undo()`

---

### `ToggleSwitchCommand`
- Implements `Command`.
- Encapsulates a switch toggle operation on `RailwayModel`.
- `execute()` and `undo()` both call `model.toggleSwitch(id)` (toggle is self-inverse).

---

## Element Registration

Elements are registered by ID with a bounding `Rectangle`:

```java
panel.registerElement("W1", new Rectangle(50, 80, 120, 40));  // Switch
panel.registerElement("S1", new Rectangle(350, 70, 50, 50));  // Signal