# Control public state mutation

Public object state will be inspected through read-only values and changed through explicit mutation methods, allowing validation, dirty tracking, and derived-state maintenance at the owning component boundary. This deliberately gives up mutable JOML chaining in the public API; large geometry payloads will instead receive a separate explicit bulk-edit contract so performance does not require silently exposing mutable internal state.
