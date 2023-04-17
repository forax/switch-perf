package com.github.forax.switchperf;

import java.util.concurrent.TimeUnit;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.IntStream;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Benchmark                       Mode  Cnt     Score   Error  Units
 * Bench.A_switch_array_of_I       avgt   10   804.145 ± 1.859  us/op
 * Bench.A_virtual_array_of_I      avgt   10  1245.349 ± 0.734  us/op
 * Bench.B_switch_array_of_A       avgt   10    33.033 ± 0.018  us/op
 * Bench.B_virtual_array_of_A      avgt   10    32.798 ± 0.018  us/op
 * Bench.C_switch_if_array_of_I    avgt   10  1058.141 ± 2.276  us/op
 * Bench.C_switch_when_array_of_I  avgt   10  1504.682 ± 5.221  us/op
 * Bench.D_switch_if_array_of_A    avgt   10    42.967 ± 0.148  us/op
 * Bench.D_switch_when_array_of_A  avgt   10    43.250 ± 0.100  us/op
 */
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgs = "--enable-preview")
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class Bench {
  private sealed interface I {
    int method();
  }
  record A(int value) implements I {
    @Override
    public int method() {
      return value;
    }
  }
  record B(long value) implements I {
    @Override
    public int method() {
      return (int) value;
    }
  }
  record C(String value) implements I {
    @Override
    public int method() {
      return value.length();
    }
  }

  static int switchMethod(I i) {
    return switch (i) {
      case A a -> a.value;
      case B b -> (int) b.value;
      case C c -> c.value.length();
    };
  }


  static int switchIfMethod(I i) {
    return switch (i) {
      case A a -> {
        if ((a.value & 1) == 0) {
          yield 2 * a.value;
        }
        yield a.value;
      }
      case B b ->  {
        if ((b.value & 1) == 0) {
          yield 2 * (int) b.value;
        }
        yield (int) b.value;
      }
      case C c -> {
        if ((c.value.length() & 1) == 0) {
          yield 2 * c.value.length();
        }
        yield c.value.length();
      }
    };
  }

  static int switchWhenMethod(I i) {
    return switch (i) {
      case A a when (a.value & 1) == 0 -> 2 * a.value;
      case A a -> a.value;
      case B b when (b.value & 1) == 0 -> 2 * (int) b.value;
      case B b -> (int) b.value;
      case C c when (c.value.length() & 1) == 0 -> 2 * c.value.length();
      case C c -> c.value.length();
    };
  }


  private I[] arrayOfI;
  private A[] arrayOfA;

  @Setup
  public void init() {
    var random = RandomGeneratorFactory.getDefault().create(0);
    arrayOfI = random.ints(100_000, 0, 300)
        .mapToObj(i -> switch (i % 3) {
          case 0 -> new A(i);
          case 1 -> new B(i);
          case 2 -> new C("" + i);
          default -> throw new AssertionError();
        })
        .toArray(I[]::new);
    arrayOfA = random.ints(100_000, 0, 300)
        .mapToObj(A::new)
        .toArray(A[]::new);
  }



  @Benchmark
  public int A_virtual_array_of_I() {
    var sum = 0;
    for(var element: arrayOfI) {
      sum += element.method();
    }
    return sum;
  }

  @Benchmark
  public int A_switch_array_of_I() {
    var sum = 0;
    for(var element: arrayOfI) {
      sum += switchMethod(element);
    }
    return sum;
  }


  @Benchmark
  public int B_virtual_array_of_A() {
    var sum = 0;
    for(var element: arrayOfA) {
      sum += element.method();
    }
    return sum;
  }

  @Benchmark
  public int B_switch_array_of_A() {
    var sum = 0;
    for(var element: arrayOfA) {
      sum += switchMethod(element);
    }
    return sum;
  }

  @Benchmark
  public int C_switch_if_array_of_I() {
    var sum = 0;
    for(var element: arrayOfI) {
      sum += switchIfMethod(element);
    }
    return sum;
  }

  @Benchmark
  public int C_switch_when_array_of_I() {
    var sum = 0;
    for(var element: arrayOfI) {
      sum += switchWhenMethod(element);
    }
    return sum;
  }

  @Benchmark
  public int D_switch_if_array_of_A() {
    var sum = 0;
    for(var element: arrayOfA) {
      sum += switchIfMethod(element);
    }
    return sum;
  }

  @Benchmark
  public int D_switch_when_array_of_A() {
    var sum = 0;
    for(var element: arrayOfA) {
      sum += switchWhenMethod(element);
    }
    return sum;
  }
}
