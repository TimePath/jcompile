#define ARRAY_COMPONENT(name, i, v)                             \
    float name##_##i = v;                                       \
    float name##_get_##i() { return name##_##i; }               \
    float name##_set_##i(float value##i) { return name##_##i = value##i; }

#define ARRAY(name, size)                                       \
    float name##_size = size;                                   \
    float name##_get(float index) {                             \
        if (index < 0) index += name##_size;                    \
        if (index > name##_size) return 0;                      \
        auto name##_get_this;                                   \
        name##_get_this = name##_get + ftoi(2 * (1 + index));   \
        return name##_get_this();                               \
    }                                                           \
    float name##_set(float index, float value) {                \
        if (index < 0) index += name##_size;                    \
        if (index > name##_size) return 0;                      \
        auto name##_set_this;                                   \
        name##_set_this = name##_set + ftoi(2 * (1 + index));   \
        return name##_set_this(value);                          \
    }

ARRAY(array, 256)
#include <boost/preprocessor/repetition/repeat.hpp>
#define DECL(z, n, name) ARRAY_COMPONENT(name, n, n)
BOOST_PP_REPEAT(256, DECL, array)

void main() {
    float f;
    for(f = 0; f < array_size; ++f) {
        float v = array_get(f);
        assert(v == f, "Initial value");
    }
    assert(array_set(0, 2) == 2, "Result of assignment");
    assert(array_get(0) == 2, "Read");
}
