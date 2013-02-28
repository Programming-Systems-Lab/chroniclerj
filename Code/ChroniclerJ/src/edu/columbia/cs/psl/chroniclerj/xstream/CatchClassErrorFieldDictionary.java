
package edu.columbia.cs.psl.chroniclerj.xstream;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.thoughtworks.xstream.converters.reflection.FieldDictionary;
import com.thoughtworks.xstream.converters.reflection.FieldKey;
import com.thoughtworks.xstream.converters.reflection.FieldKeySorter;
import com.thoughtworks.xstream.converters.reflection.ImmutableFieldKeySorter;
import com.thoughtworks.xstream.converters.reflection.MissingFieldException;
import com.thoughtworks.xstream.converters.reflection.ObjectAccessException;
import com.thoughtworks.xstream.core.Caching;
import com.thoughtworks.xstream.core.JVM;
import com.thoughtworks.xstream.core.util.OrderRetainingMap;

public class CatchClassErrorFieldDictionary extends FieldDictionary {
    private transient Map keyedByFieldNameCache;

    private transient Map keyedByFieldKeyCache;

    private final FieldKeySorter sorter;

    public CatchClassErrorFieldDictionary() {
        this(new ImmutableFieldKeySorter());
    }

    public CatchClassErrorFieldDictionary(FieldKeySorter sorter) {
        this.sorter = sorter;
        init();
    }

    private void init() {
        keyedByFieldNameCache = new HashMap();
        keyedByFieldKeyCache = new HashMap();
        keyedByFieldNameCache.put(Object.class, Collections.EMPTY_MAP);
        keyedByFieldKeyCache.put(Object.class, Collections.EMPTY_MAP);
    }

    /**
     * Returns an iterator for all fields for some class
     * 
     * @param cls the class you are interested on
     * @return an iterator for its fields
     * @deprecated As of 1.3, use {@link #fieldsFor(Class)} instead
     */
    public Iterator serializableFieldsFor(Class cls) {
        return fieldsFor(cls);
    }

    /**
     * Returns an iterator for all fields for some class
     * 
     * @param cls the class you are interested on
     * @return an iterator for its fields
     */
    public Iterator fieldsFor(final Class cls) {
        return buildMap(cls, true).values().iterator();
    }

    /**
     * Returns an specific field of some class. If definedIn is null, it
     * searches for the field named 'name' inside the class cls. If definedIn is
     * different than null, tries to find the specified field name in the
     * specified class cls which should be defined in class definedIn (either
     * equals cls or a one of it's superclasses)
     * 
     * @param cls the class where the field is to be searched
     * @param name the field name
     * @param definedIn the superclass (or the class itself) of cls where the
     *            field was defined
     * @return the field itself
     * @throws ObjectAccessException if no field can be found
     */
    public Field field(Class cls, String name, Class definedIn) {
        Field field = fieldOrNull(cls, name, definedIn);
        if (field == null) {
            throw new MissingFieldException(cls.getName(), name);
        } else {
            return field;
        }
    }

    /**
     * Returns an specific field of some class. If definedIn is null, it
     * searches for the field named 'name' inside the class cls. If definedIn is
     * different than null, tries to find the specified field name in the
     * specified class cls which should be defined in class definedIn (either
     * equals cls or a one of it's superclasses)
     * 
     * @param cls the class where the field is to be searched
     * @param name the field name
     * @param definedIn the superclass (or the class itself) of cls where the
     *            field was defined
     * @return the field itself or <code>null</code>
     * @since 1.4
     */
    public Field fieldOrNull(Class cls, String name, Class definedIn) {
        Map fields = buildMap(cls, definedIn != null);
        Field field = (Field) fields.get(definedIn != null ? (Object) new FieldKey(name, definedIn,
                0) : (Object) name);
        return field;
    }

    @SuppressWarnings({
            "rawtypes", "unchecked"
    })
    private Map buildMap(final Class type, boolean tupleKeyed) {
        final Map result;
        Class cls = type;
        // synchronized (this) {
        if (!keyedByFieldNameCache.containsKey(type)) {
            final List superClasses = new ArrayList();
            while (!Object.class.equals(cls)) {
                superClasses.add(0, cls);
                cls = cls.getSuperclass();
            }
            Map lastKeyedByFieldName = Collections.EMPTY_MAP;
            Map lastKeyedByFieldKey = Collections.EMPTY_MAP;
            for (final Iterator iter = superClasses.iterator(); iter.hasNext();) {
                cls = (Class) iter.next();
                if (!keyedByFieldNameCache.containsKey(cls)) {
                    final Map keyedByFieldName = new HashMap(lastKeyedByFieldName);
                    final Map keyedByFieldKey = new OrderRetainingMap(lastKeyedByFieldKey);
                    try {
                        Field[] fields = cls.getDeclaredFields();
                        if (JVM.reverseFieldDefinition()) {
                            for (int i = fields.length >> 1; i-- > 0;) {
                                final int idx = fields.length - i - 1;
                                final Field field = fields[i];
                                fields[i] = fields[idx];
                                fields[idx] = field;
                            }
                        }
                        for (int i = 0; i < fields.length; i++) {
                            Field field = fields[i];
                            if (!field.isAccessible()) {
                                field.setAccessible(true);
                            }
                            FieldKey fieldKey = new FieldKey(field.getName(),
                                    field.getDeclaringClass(), i);
                            Field existent = (Field) keyedByFieldName.get(field.getName());
                            if (existent == null
                            // do overwrite statics
                                    || ((existent.getModifiers() & Modifier.STATIC) != 0)
                                    // overwrite non-statics with non-statics
                                    // only
                                    || (existent != null && ((field.getModifiers() & Modifier.STATIC) == 0))) {
                                keyedByFieldName.put(field.getName(), field);
                            }
                            keyedByFieldKey.put(fieldKey, field);
                        }
                    } catch (NoClassDefFoundError ex) {
                        // do nothing!
                    }
                    final Map sortedFieldKeys = sorter.sort(type, keyedByFieldKey);
                    keyedByFieldNameCache.put(cls, keyedByFieldName);
                    keyedByFieldKeyCache.put(cls, sortedFieldKeys);
                    lastKeyedByFieldName = keyedByFieldName;
                    lastKeyedByFieldKey = sortedFieldKeys;
                } else {
                    lastKeyedByFieldName = (Map) keyedByFieldNameCache.get(cls);
                    lastKeyedByFieldKey = (Map) keyedByFieldKeyCache.get(cls);
                }
            }
            result = tupleKeyed ? lastKeyedByFieldKey : lastKeyedByFieldName;
        } else {
            result = (Map) (tupleKeyed ? keyedByFieldKeyCache.get(type) : keyedByFieldNameCache
                    .get(type));
        }
        // }
        return result;
    }

    public void flushCache() {
        Set objectTypeSet = Collections.singleton(Object.class);
        keyedByFieldNameCache.keySet().retainAll(objectTypeSet);
        keyedByFieldKeyCache.keySet().retainAll(objectTypeSet);
        if (sorter instanceof Caching) {
            ((Caching) sorter).flushCache();
        }
    }

    protected Object readResolve() {
        init();
        return this;
    }
}
