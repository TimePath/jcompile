#define ARRAY_COMPONENT(type, name, i, v)                                                   \
    type name##_##i = v;                                                                    \
    type name##_access_##i(bool mode, type value##i) {                                      \
        return mode ? name##_##i = value##i : name##_##i;                                   \
    }

#define ARRAY(type, name, size)                                                             \
    type name##_size = size;                                                                \
    type(bool, type) name##_access(type index) {                                            \
      /*if (index < 0) index += name##_size;                                                \
        if (index > name##_size) return 0;*/                                                \
        return *(&name##_access + 1 + index);                                               \
    }

ARRAY(float, array, 2)
#include <boost/preprocessor/repetition/repeat.hpp>
#define DECL(z, n, name) ARRAY_COMPONENT(float, name, n, n)
BOOST_PP_REPEAT(2, DECL, array)

void main() {
    for(float f = 0; f < array_size; ++f) {
        float(bool, float) v = array_access(f);
        assert(v(0) == f, "Initial value");
    }
    float(bool, float) index = array_access(0);
    assert(index(1, 2) == 2, "Result of assignment");
    assert(index(0) == 2, "Read");
}
