import com.edwards.orm.annotation.validation.Size;
import com.edwards.orm.annotation.validation.Validator;
import com.edwards.orm.annotation.validation.Violation;
import org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class ValidatorTest {
    private static class TestObject {
        @Size(min = 3, max = 10)
        private String value;

        private TestObject(String value) {
            this.value = value;
        }

        public static TestObject getAsCorrect() {
            return new TestObject("aaaa");
        }

        public static TestObject getAsWrong() {
            return new TestObject("aa");
        }
    }

    @Test
    public void testCorrectClassValidation() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        List<Violation> violations = Validator.validateObject(TestObject.getAsCorrect());
        assert violations.isEmpty();
    }

    @Test
    public void testWrongClassValidation() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        List<Violation> violations = Validator.validateObject(TestObject.getAsWrong());
        assert !violations.isEmpty();
    }
}
