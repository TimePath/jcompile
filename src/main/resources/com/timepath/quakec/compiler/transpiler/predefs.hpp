#pragma once
typedef const char *string;

inline string _(string str) {
    return str;
}

enum component {
    x, y, z
};

struct vector {
    float x, y, z;

    float operator[](component c) {
        switch (c) {
            case ::x:
                return x;
            case ::y:
                return y;
            case ::z:
                return z;
        }
        return 0;
    }

    vector operator+(const vector &other) {
        return (vector) {x + other.x, y + other.y, z + other.z};
    }

    vector &operator+=(const vector &other) {
        vector self = *this;
        self.x += other.x;
        self.y += other.y;
        self.z += other.z;
        return self;
    }

    vector operator-(const vector &other) {
        return (vector) {x - other.x, y - other.y, z - other.z};
    }

    vector &operator-=(const vector &other) {
        vector self = *this;
        self.x -= other.x;
        self.y -= other.y;
        self.z -= other.z;
        return self;
    }

    vector operator*(const float other) {
        return (vector) {x * other, y * other, z * other};
    }

    vector &operator*=(const float other) {
        vector self = *this;
        self.x *= other;
        self.y *= other;
        self.z *= other;
        return self;
    }

    float operator*(const vector &other) {
        return x * other.x + y * other.y + z * other.z;
    }

    vector operator/(const float other) {
        return (vector) {x / other, y / other, z / other};
    }

    vector &operator/=(const float other) {
        vector self = *this;
        self.x /= other;
        self.y /= other;
        self.z /= other;
        return self;
    }

    explicit operator bool() {
        return !(x == 0 && y == 0 && z == 0);
    }

    bool operator==(const vector &other) {
        return x == other.x && y == other.y && z == other.z;
    }

    bool operator!=(const vector &other) {
        return !(x == other.x && y == other.y && z == other.z);
    }

    vector operator&(const vector &other) {
        return (vector) {x & other, y & other, z & other};
    }

    vector &operator&=(const vector &other) {
        vector self = *this;
        self.x &= other.x;
        self.y &= other.y;
        self.z &= other.z;
        return self;
    }

    vector operator|(const vector &other) {
        return (vector) {x | other, y | other, z | other};
    }

    vector &operator|=(const vector &other) {
        vector self = *this;
        self.x |= other.x;
        self.y |= other.y;
        self.z |= other.z;
        return self;
    }

    vector operator~() {
        return (vector) {~x, ~y, ~z};
    }

};

vector operator*(float f, const vector &other) const {
    return (vector) {f * other.x, f * other.y, f * other.z};
}

struct entity {

    template<typename T>
    T &operator[](T *field) {
        return T();
    }

    operator bool() {
        return true;
    }

};

/*
class number {
    union {
        float f;
        int i;
        bool b;
    } n;
    bool ok_;

    typedef void (number::*bool_type)() const;

    void this_type_does_not_support_comparisons() const {
    }

public:
    explicit number(bool b = true) : ok_(b) {
        n.b = b;
    }

    operator bool_type() const {
        return ok_ ? &number::this_type_does_not_support_comparisons : 0;
    }

    number() {

    }

    number(float f) {
        n.f = f;
    }

    operator float() {
        return n.f;
    }

    number *operator=(number r) {
        n = r.n;
        return this;
    }
};

template<typename T>
bool operator!=(const number &lhs, const T &rhs) {
    lhs.this_type_does_not_support_comparisons();
    return false;
}

template<typename T>
bool operator==(const number &lhs, const T &rhs) {
    lhs.this_type_does_not_support_comparisons();
    return false;
}

#define float number
#define int number
*/
