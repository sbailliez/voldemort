package voldemort.contrib.config.yaml;

import javax.validation.*;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.Set;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Helper class to perform validatio using JSR-303.
 */
public class Validations {

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static final String NL = System.getProperty("line.separator");

    @Target({TYPE, ANNOTATION_TYPE, METHOD})
    @Retention(RUNTIME)
    @Constraint(validatedBy = Validations.MethodValidator.class)
    @Documented
    public @interface Method {
        String message() default "is not valid";
        Class<?>[] groups() default {}; // mandatory attribute for annotation
        Class<?>[] payload() default {}; // mandatory attribute for annotation
    }

    /**
     * Validator to perform validation on a method annotated with Validations.Method
     */
    public static class MethodValidator implements ConstraintValidator<Method, Boolean> {

        public void initialize(Method constraintAnnotation) {}

        public boolean isValid(Boolean value, ConstraintValidatorContext context) {
            return (value == null) || value;
        }
    }

    /**
     * Perform validation of a bean.
     *
     * @param that bean to validate. Cannot be null.
     * @return the bean that was passed as an argument.
     */
    public static <T> T validate(T that) {
        Set<ConstraintViolation<T>> violations = validator.validate(that);
        if (violations.size() > 0) {
            String message = getPrettyMessage(violations);
            throw new ConstraintViolationException(message, new HashSet<ConstraintViolation<?>>(violations));
        }
        return that;
    }

    /**
     * Return a 'pretty' message that is human readable and list all validation errors. There is no point in
     * calling this method with an empty set.
     *
     * @param violations the list of violations
     * @return the human readable message
     */
    public static <T> String getPrettyMessage(Set<ConstraintViolation<T>> violations) {
        if (violations.isEmpty()) {
            return "No constraint violations detected on configuration";
        }
        StringBuilder message = new StringBuilder("Failed to validate configuration. The following errors are reported:")
                .append(NL);
        for (ConstraintViolation<T> violation : violations) {
            message.append("  - ").append(violation.getMessage()).append(NL);
        }
        return message.toString();
    }
}
