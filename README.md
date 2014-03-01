specialize-java
===============

Generics with primitive types in Java.

Preparation
-----------

If you are using maven, add the following snippet for your pom.
```xml
<repositories>
	<repository>
		<id>thisptr.net</id>
		<name>thisptr.net</name>
		<url>http://nexus.thisptr.net/content/groups/public</url>
	</repository>
</repositories>
<dependency>
	<groupId>net.thisptr</groupId>
	<artifactId>specialize-java</artifactId>
	<version>0.0.1-SNAPSHOT</version>
</dependency>
```

Usage
-----

### Generating the specializations

#### Example

Let's see a simple example. This Array class (can be a top-level or inner class) with @Specialize annotation,

```java
import net.thisptr.specialize.Specialize;
@Specialize("T: int, char")
public class Array<T> {
  public T[] values;
  public Array(int n) { values = new T[n]; }
  public T sum() {
    T result = 0;
    for (T value : values)
      result += value;
    return result;
  }
}
```

compiles to the following 3 classes (by running `javac -cp specialize-java.jar Array.java` or `mvn compile` if you have pom.xml):

```java
public class Array$specialized$int {
  public int[] buf;
  public Array(int n) { values = new int[n]; }
  public int sum() {
    int result = 0;
    for (int value : values)
      result += value;
    return result;
  }
}
```

```java
public class Array$specialized$char {
  public char[] buf;
  public Array(int n) { values = new char[n]; }
  public char sum() {
    char result = 0;
    for (char value : values)
      result += value;
    return result;
  }
}
```

```java
public class Array<T> {/*
  // By default, the original generic class is emptied,
  // because otherwise it would cause compilation errors.
  public T[] buf;
  public Array(int n) { values = new T[n]; }
  public T sum() {
    T result = 0; // we can't assign 0 to variable of type T
    for (T value : values)
      result += value; // a generic addition...?
    return result;
  }
*/}
```

Note that the @Specialize annotation and its import statement is removed, allowing specialize-java to be safely removed at runtime.

You can also add @Specialize(...) to methods, which will generate specialized overloads of the method.

```java
public class Example {
  @Specialize("T: char, int, short, byte, long")
  public static long sum(T[] values) {
    long result = 0;
    for (T value : values)
      result += value;
    return result;
  }
}
```


##### Supported @Specialize syntax
  
- @Specialize("T: int, char")  -- As the example above, this generates specializations to int and char types.
- @Specialize("T: *")  -- Generates specializations to all primitive types.
- @Specialize("T: ?")  -- Keep the original generic class, preventing it from being emptied.
- @Specialize("T: *, ?")  -- Generate specializations to all primitive types, while keeping the original generic class.


### Using the specialized types

You can use specialized types using $-prefixed primitive type names: $int, $boolean, ...

```java
public class Main {
  public static void main(String[] args) {
    Array<$int> a = new Array<$int>(10);
    System.out.println(a.sum()); // => 0
  }

  @Specialize("T: int, char")
  public static T sum(Array<T> a) {
    return a.sum();
  }
}
```

See [src/test/java/net/thisptr/specialize/example/Example.java](https://github.com/eiiches/specialize-java/blob/develop/src/test/java/net/thisptr/specialize/example/Example.java) for more.

### Specializing the standard classes in java.*

TBD

- java.util.Iterator<T>
- specialize-java-rt


Projects using this library
---------------------------

- [ml4j-strings](https://github.com/eiiches/ml4j-strings) - A set of string algorithms in java

Limitations
-----------

#### Diamond operator in Java 7

This does not compile currently.
```java
Array<$int> a = new Array<>(10)
```

#### Eclipse compiler (ecj)

Eclipse compiler (ecj) is not supported.
