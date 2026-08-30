# Expose polled input state

`Window.pollEvents()` is a process-wide operation that returns `void` and
dispatches pending platform events for every open JScene3D Window. Internal
callbacks update the affected Window and its stable, read-only `InputState`.

An `InputState` reports persistent held keys, held mouse buttons, and current
pointer position. It also reports press and release transitions, accumulated
pointer movement, and accumulated scrolling for the latest poll. Transient
state is cleared at the beginning of the next poll, and reading it never
consumes it.

This model keeps the normal render loop allocation-free, gives multiple
application components a consistent view of one polling cycle, and reflects
GLFW's process-wide event dispatch. Version 0.1 does not expose allocated event
lists, native callbacks, text input, file drops, or ordered event replay; those
interfaces require concrete examples with semantics that `InputState` cannot
represent.
