#define ARRAY_COMPONENT(name, i, v)                                                         \
    float name##_##i = v;                                                                   \
    float name##_access_##i(bool mode, float value##i) {                                    \
        return mode ? name##_##i = value##i : name##_##i;                                   \
    }

#define ARRAY(name, size)                                                                   \
    float name##_size = size;                                                               \
    float(bool, float) name##_access(float index) {                                         \
      /*if (index < 0) index += name##_size;                                                \
        if (index > name##_size) return 0;*/                                                \
        float(bool, float) name##_access_this = name##_access + *(1 + index);               \
        return name##_access_this;                                                          \
    }

ARRAY(array, 256)
#include <boost/preprocessor/repetition/repeat.hpp>
#define DECL(z, n, name) ARRAY_COMPONENT(name, n, n)
BOOST_PP_REPEAT(256, DECL, array)

void main() {
    for(float f = 0; f < array_size; ++f) {
        float(bool, float) v = array_access(f);
        assert(v(0) == f, "Initial value");
    }
    float(bool, float) index = array_access(0);
    assert(index(1, 2) == 2, "Result of assignment");
    assert(index(0) == 2, "Read");
}
