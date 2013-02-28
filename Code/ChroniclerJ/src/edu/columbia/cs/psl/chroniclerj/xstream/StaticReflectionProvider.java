
package edu.columbia.cs.psl.chroniclerj.xstream;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;

import com.thoughtworks.xstream.converters.reflection.ObjectAccessException;
import com.thoughtworks.xstream.converters.reflection.Sun14ReflectionProvider;

public class StaticReflectionProvider extends Sun14ReflectionProvider {
    public void writeField(Object object, String fieldName, Object value, Class definedIn) {
        if (!Modifier.isStatic(fieldDictionary.field(object.getClass(), fieldName, definedIn)
                .getModifiers())) {
            super.writeField(object, fieldName, value, definedIn);
        } else {
            try {
                fieldDictionary.field(object.getClass(), fieldName, definedIn).set(null, value);
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public StaticReflectionProvider() {
        super(new CatchClassErrorFieldDictionary());
    }

    @Override
    public void visitSerializableFields(Object object, Visitor visitor) {
        for (Iterator iterator = fieldDictionary.fieldsFor(object.getClass()); iterator.hasNext();) {
            Field field = (Field) iterator.next();
            if (!fieldModifiersSupported(field)) {
                continue;
            }
            validateFieldAccess(field);
            try {
                Object value = field.get(object);
                if (value != null)
                    // synchronized (value) {
                    visitor.visit(field.getName(), field.getType(), field.getDeclaringClass(),
                            value);
                // }
                else
                    visitor.visit(field.getName(), field.getType(), field.getDeclaringClass(),
                            value);

            } catch (IllegalArgumentException e) {
                throw new ObjectAccessException("Could not get field " + field.getClass() + "."
                        + field.getName(), e);
            } catch (IllegalAccessException e) {
                throw new ObjectAccessException("Could not get field " + field.getClass() + "."
                        + field.getName(), e);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected boolean fieldModifiersSupported(Field field) {
        int modifiers = field.getModifiers();
        return !(Modifier.isTransient(modifiers) || (Modifier.isStatic(modifiers) && Modifier
                .isFinal(modifiers)));
    }
}
