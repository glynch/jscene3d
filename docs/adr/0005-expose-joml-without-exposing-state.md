# Expose JOML without exposing mutable state

JOML is an intentional part of the public interface rather than an implementation hidden behind pass-through math wrappers. Library-owned math state is exposed through zero-copy JOML read-only interfaces and changed only through controlled methods; common mutations offer both scalar overloads that require no temporary object and read-only JOML-value overloads that copy an existing value, while explicit snapshot operations copy into caller-owned destinations when stable values are required.
