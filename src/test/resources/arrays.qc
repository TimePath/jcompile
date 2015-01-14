#define ARRAY_COMPONENT(name, i, v)                         \
    float name##_##i = v;                                   \
    float name##_get_##i() { return name##_##i; }           \
    float name##_set_##i(float value##i) { return name##_##i = value##i; }

#define ARRAY(name, size)                                   \
    float name##_size = size;                               \
    float name##_get(float index) {                         \
        if (index < 0) index += name##_size;                \
        if (index > name##_size) return 0;                  \
        ++index;                                            \
        auto name##_get_this;                               \
        name##_get_this = name##_get + ftoi(2 * index);     \
        return name##_get_this();                           \
    }                                                       \
    float name##_set(float index2, float value) {           \
        if (index2 < 0) index2 += name##_size;              \
        if (index2 > name##_size) return 0;                 \
        ++index2;                                           \
        auto name##_set_this;                               \
        name##_set_this = name##_set + ftoi(2 * index2);    \
        return name##_set_this(value);                      \
    }

ARRAY(array, 3)
ARRAY_COMPONENT(array, 0, 0)
ARRAY_COMPONENT(array, 1, 0)
ARRAY_COMPONENT(array, 2, 0)

void main() {
    assert(array_get(0) == 0, "Initial value");
    assert(array_set(0, 2) == 2, "Result of assignment");
    assert(array_get(0) == 2, "Read");
}
