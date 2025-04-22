//import java.util.function.Function;
//import java.util.function.Supplier;
//
//public class TestConfig {
//
//
//    public enum ConfigKey {
//
//        foo(Boolean.class, Boolean::parseBoolean),
//        bar(Integer.class, Integer::parseInt);
//
//        private Class<?> clazz;
//        private Function<String, ?> op;
//
//        ConfigKey(Class<?> clazz, Function<String, ?> op) {
//            this.clazz = clazz;
//            this.op = op;
//        }
//
//        public Class<?> apply(String value) {
//            return (Class<?>) op.apply(value);
//        }
//    }
//
//    void main(String[] args) {
//
//        boolean a = ConfigKey.foo.apply("aaa");
//
//    }
//
//
//
////    public enum ConfigKey<T> {
////
////        foo(Boolean.class, s -> () -> Boolean.parseBoolean(s));
////
////        private final Class<T> clazz;
////        private final Function<String, Supplier<T>> op;
////
////        ConfigKey(Class<T> clazz, Function<String, Supplier<T>> op) {
////            this.clazz = clazz;
////            this.op = op;
////        }
////
////        public Function<String, Supplier<T>> getOp() {
////            return op;
////        }
////    }
//
//}
