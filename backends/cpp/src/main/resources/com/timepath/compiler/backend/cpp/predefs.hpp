using std::string;

inline string _(string str) {
    return str;
}

// Brings members of a vector into the current namespace to be used with pointer to member
#define VECTOR(name)                        \
union {                                     \
    vector name;                            \
    struct {                                \
        [[deprecated("Use member access")]] \
        float name##_x, name##_y, name##_z; \
    };                                      \
}

struct vector {
    float x, y, z;

    vector() : x(0), y(0), z(0) { }

    vector(float x, float y, float z) : x(x), y(y), z(z) { }

    vector operator+() {
        return vector(x, y, z);
    }

    vector operator+(const vector &other) {
        return vector(x + other.x, y + other.y, z + other.z);
    }

    vector &operator+=(const vector &other) {
        vector self = *this;
        self.x += other.x;
        self.y += other.y;
        self.z += other.z;
        return *this;
    }

    vector operator-() {
        return vector(-x, -y, -z);
    }

    vector operator-(const vector &other) {
        return vector(x - other.x, y - other.y, z - other.z);
    }

    vector &operator-=(const vector &other) {
        vector self = *this;
        self.x -= other.x;
        self.y -= other.y;
        self.z -= other.z;
        return *this;
    }

    vector operator*(const float other) {
        return vector(x * other, y * other, z * other);
    }

    vector &operator*=(const float other) {
        vector self = *this;
        self.x *= other;
        self.y *= other;
        self.z *= other;
        return *this;
    }

    float operator*(const vector &other) {
        return x * other.x + y * other.y + z * other.z;
    }

    vector operator/(const float other) {
        return vector(x / other, y / other, z / other);
    }

    vector &operator/=(const float other) {
        vector self = *this;
        self.x /= other;
        self.y /= other;
        self.z /= other;
        return *this;
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
        return vector(
                        (float) (((int) x) & ((int) other.x)),
                        (float) (((int) y) & ((int) other.y)),
                        (float) (((int) z) & ((int) other.z))
                        );
    }

    vector &operator&=(const vector &other) {
        vector self = *this;
        self.x = (float) ((int) self.x & (int) other.x);
        self.y = (float) ((int) self.y & (int) other.y);
        self.z = (float) ((int) self.z & (int) other.z);
        return *this;
    }

    vector operator|(const vector &other) {
        return vector(
                        (float) (((int) x) | ((int) other.x)),
                        (float) (((int) y) | ((int) other.y)),
                        (float) (((int) z) | ((int) other.z))
                        );
    }

    vector &operator|=(const vector &other) {
        vector self = *this;
        self.x = (float) ((int) self.x | (int) other.x);
        self.y = (float) ((int) self.y | (int) other.y);
        self.z = (float) ((int) self.z | (int) other.z);
        return *this;
    }

    vector operator~() {
        return vector((float)~(int) x, (float)~(int) y, (float)~(int) z);
    }

};

vector operator*(float f, const vector &other) {
    return vector(f * other.x, f * other.y, f * other.z);
}

struct entity_base {

    operator bool() {
        return true;
    }

};
