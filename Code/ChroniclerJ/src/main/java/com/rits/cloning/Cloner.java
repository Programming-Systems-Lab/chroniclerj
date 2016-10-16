
package com.rits.cloning;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.activation.FileDataSource;

/**
 * Cloner: deep clone objects. This class is thread safe. One instance can be
 * used by multiple threads on the same time.
 * 
 * @author kostantinos.kougios 18 Sep 2008
 */
public class Cloner {
    private final IInstantiationStrategy instantiationStrategy;

    private final Set<Class<?>> ignored = new HashSet<Class<?>>();

    private final Set<Class<?>> ignoredInstanceOf = new HashSet<Class<?>>();

    private final Set<Class<?>> nullInstead = new HashSet<Class<?>>();

    private final Map<Class<?>, IFastCloner> fastCloners = new HashMap<Class<?>, IFastCloner>();

    private final Map<Object, Boolean> ignoredInstances = new IdentityHashMap<Object, Boolean>();

    private final ConcurrentHashMap<Class<?>, List<Field>> fieldsCache = new ConcurrentHashMap<Class<?>, List<Field>>();

    private boolean dumpClonedClasses = false;

    private boolean cloningEnabled = true;

    private boolean nullTransient = false;

    private boolean cloneSynthetics = true;

    private boolean dumpCloneStatistics = true;

    private final static HashMap<Class<?>, Long> timePerClass = new HashMap<>();

    public Cloner() {
        this.instantiationStrategy = ObjenesisInstantiationStrategy.getInstance();
        init();
    }

    public Cloner(final IInstantiationStrategy instantiationStrategy) {
        this.instantiationStrategy = instantiationStrategy;
        init();
    }

    public boolean isNullTransient() {
        return nullTransient;
    }

    /**
     * this makes the cloner to set a transient field to null upon cloning.
     * NOTE: primitive types can't be nulled. Their value will be set to
     * default, i.e. 0 for int
     * 
     * @param nullTransient true for transient fields to be nulled
     */
    public void setNullTransient(final boolean nullTransient) {
        this.nullTransient = nullTransient;
    }

    public void setCloneSynthetics(final boolean cloneSynthetics) {
        this.cloneSynthetics = cloneSynthetics;
    }

    private void init() {
        registerKnownJdkImmutableClasses();
        registerKnownConstants();
        registerFastCloners();
    }

    /**
     * registers a std set of fast cloners.
     */
    protected void registerFastCloners() {
        fastCloners.put(GregorianCalendar.class, new FastClonerCalendar());
        fastCloners.put(ArrayList.class, new FastClonerArrayList());
        fastCloners.put(LinkedList.class, new FastClonerLinkedList());
        fastCloners.put(HashSet.class, new FastClonerHashSet());
        fastCloners.put(HashMap.class, new FastClonerHashMap());
        fastCloners.put(TreeMap.class, new FastClonerTreeMap());
        fastCloners.put(ConcurrentHashMap.class, new FastClonerConcurrentHashMap());
    }

    protected Object fastClone(final Object o, final Map<Object, Object> clones)
            throws IllegalAccessException {
        final Class<? extends Object> c = o.getClass();
        final IFastCloner fastCloner = fastCloners.get(c);
        if (fastCloner != null)
            return fastCloner.clone(o, this, clones);
        return null;
    }

    public void registerConstant(final Object o) {
        ignoredInstances.put(o, true);
    }

    public void registerConstant(final Class<?> c, final String privateFieldName) {
        try {
            final Field field = c.getDeclaredField(privateFieldName);
            field.setAccessible(true);
            final Object v = field.get(null);
            ignoredInstances.put(v, true);
        } catch (final SecurityException e) {
            throw new RuntimeException(e);
        } catch (final NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (final IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (final IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * registers some known JDK immutable classes. Override this to register
     * your own list of jdk's immutable classes
     */
    protected void registerKnownJdkImmutableClasses() {
        registerImmutable(String.class);
        registerImmutable(Integer.class);
        registerImmutable(Long.class);
        registerImmutable(Boolean.class);
        registerImmutable(Class.class);
        registerImmutable(Float.class);
        registerImmutable(Double.class);
        registerImmutable(Character.class);
        registerImmutable(Byte.class);
        registerImmutable(Short.class);
        registerImmutable(Void.class);

        registerImmutable(BigDecimal.class);
        registerImmutable(BigInteger.class);
        registerImmutable(URI.class);
        registerImmutable(URL.class);
        registerImmutable(UUID.class);
        registerImmutable(Pattern.class);
    }

    protected void registerKnownConstants() {
        // registering known constants of the jdk.
        registerStaticFields(TreeSet.class, HashSet.class, HashMap.class, TreeMap.class);
    }

    /**
     * registers all static fields of these classes. Those static fields won't
     * be cloned when an instance of the class is cloned. This is useful i.e.
     * when a static field object is added into maps or sets. At that point,
     * there is no way for the cloner to know that it was static except if it is
     * registered.
     * 
     * @param classes array of classes
     */
    public void registerStaticFields(final Class<?>... classes) {
        for (final Class<?> c : classes) {
            final List<Field> fields = allFields(c);
            for (final Field field : fields) {
                final int mods = field.getModifiers();
                if (Modifier.isStatic(mods) && !field.getType().isPrimitive()) {
                    registerConstant(c, field.getName());
                }
            }
        }
    }

    /**
     * spring framework friendly version of registerStaticFields
     * 
     * @param set a set of classes which will be scanned for static fields
     */
    public void setExtraStaticFields(final Set<Class<?>> set) {
        registerStaticFields((Class<?>[]) set.toArray());
    }

    /**
     * instances of classes that shouldn't be cloned can be registered using
     * this method.
     * 
     * @param c The class that shouldn't be cloned. That is, whenever a deep
     *            clone for an object is created and c is encountered, the
     *            object instance of c will be added to the clone.
     */
    public void dontClone(final Class<?>... c) {
        for (final Class<?> cl : c) {
            ignored.add(cl);
        }
    }

    public boolean isIgnored(Class<?> clz) {
        for (final Class<?> c : ignored) {
            if (c.isAssignableFrom(clz))
                return true;
        }
        return false;
    }

    public void dontCloneInstanceOf(final Class<?>... c) {
        for (final Class<?> cl : c) {
            ignoredInstanceOf.add(cl);
        }
    }

    public void setDontCloneInstanceOf(final Class<?>... c) {
        dontCloneInstanceOf(c);
    }

    /**
     * instead of cloning these classes will set the field to null
     * 
     * @param c the classes to nullify during cloning
     */
    public void nullInsteadOfClone(final Class<?>... c) {
        for (final Class<?> cl : c) {
            nullInstead.add(cl);
        }
    }

    /**
     * spring framework friendly version of nullInsteadOfClone
     */
    public void setExtraNullInsteadOfClone(final Set<Class<?>> set) {
        nullInstead.addAll(set);
    }

    /**
     * registers an immutable class. Immutable classes are not cloned.
     * 
     * @param c the immutable class
     */
    public void registerImmutable(final Class<?>... c) {
        for (final Class<?> cl : c) {
            ignored.add(cl);
        }
    }

    /**
     * spring framework friendly version of registerImmutable
     */
    public void setExtraImmutables(final Set<Class<?>> set) {
        ignored.addAll(set);
    }

    public void registerFastCloner(final Class<?> c, final IFastCloner fastCloner) {
        if (fastCloners.containsKey(c))
            throw new IllegalArgumentException(c + " already fast-cloned!");
        fastCloners.put(c, fastCloner);
    }

    /**
     * creates a new instance of c. Override to provide your own implementation
     * 
     * @param <T> the type of c
     * @param c the class
     * @return a new instance of c
     */
    protected <T> T newInstance(final Class<T> c) {
        return instantiationStrategy.newInstance(c);
    }

    @SuppressWarnings("unchecked")
    public <T> T fastCloneOrNewInstance(final Class<T> c) {
        try {
            final T fastClone = (T) fastClone(c, null);
            if (fastClone != null)
                return fastClone;
        } catch (final IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return newInstance(c);

    }

    /**
     * deep clones "o".
     * 
     * @param <T> the type of "o"
     * @param o the object to be deep-cloned
     * @return a deep-clone of "o".
     */
    public <T> T deepClone(final T o) {
        if (o == null)
            return null;
        if (!cloningEnabled)
            return o;
        if (dumpClonedClasses) {
            System.out.println("start>" + o.getClass());
        }
        final Map<Object, Object> clones = new IdentityHashMap<Object, Object>(16);
        try {
            return cloneInternal(o, clones);
        } catch (final IllegalAccessException e) {
            throw new CloningException("error during cloning of " + o, e);
        }
    }

    public <T> T deepCloneDontCloneInstances(final T o, final Object... dontCloneThese) {
        if (o == null)
            return null;
        if (!cloningEnabled)
            return o;
        if (dumpClonedClasses) {
            System.out.println("start>" + o.getClass());
        }
        final Map<Object, Object> clones = new IdentityHashMap<Object, Object>(16);
        for (final Object dc : dontCloneThese) {
            clones.put(dc, dc);
        }
        try {
            return cloneInternal(o, clones);
        } catch (final IllegalAccessException e) {
            throw new CloningException("error during cloning of " + o, e);
        }
    }

    /**
     * shallow clones "o". This means that if c=shallowClone(o) then c!=o. Any
     * change to c won't affect o.
     * 
     * @param <T> the type of o
     * @param o the object to be shallow-cloned
     * @return a shallow clone of "o"
     */
    public <T> T shallowClone(final T o) {
        if (o == null)
            return null;
        if (!cloningEnabled)
            return o;
        try {
            return cloneInternal(o, null);
        } catch (final IllegalAccessException e) {
            throw new CloningException("error during cloning of " + o, e);
        }
    }

    // caches immutables for quick reference
    private final ConcurrentHashMap<Class<?>, Boolean> immutables = new ConcurrentHashMap<Class<?>, Boolean>();

    private boolean cloneAnonymousParent = true;

    /**
     * override this to decide if a class is immutable. Immutable classes are
     * not cloned.
     * 
     * @param clz the class under check
     * @return true to mark clz as immutable and skip cloning it
     */
    protected boolean considerImmutable(final Class<?> clz) {
        return false;
    }

    protected Class<?> getImmutableAnnotation() {
        return Immutable.class;
    }

    /**
     * decides if a class is to be considered immutable or not
     * 
     * @param clz the class under check
     * @return true if the clz is considered immutable
     */
    private boolean isImmutable(final Class<?> clz) {
        if (clz.getName().equals("java.util.HashSet")
        // clz.getName().startsWith("java.util")
        // && !clz.getName().equals("java.util.HashMap")
        // && !clz.getName().equals("java.util.LinkedHashMap")
        // && !clz.getName().startsWith("java.util.Hashtable")
        // && !clz.getName().equals("java.util.ArrayList")
        // && !clz.getName().contains("Map")
        // && ! clz.getName().startsWith("java.util.Collections")
        // //&& !clz.getName().equals("java.util.HashSet") //bad
        // && !clz.getName().startsWith("java.util.Linked")
        // && !clz.getName().contains("Vector")
        // && !clz.getName().equals("java.util.concurrent.CopyOnWriteArrayList")
        // && !clz.getName().equals("java.util.Arrays$ArrayList")
        ) {
            // System.out.println(clz.getName());
            return true;
        }
        final Boolean isIm = immutables.get(clz);
        if (isIm != null)
            return isIm;
        if (considerImmutable(clz))
            return true;

        Class<?> c = clz.getSuperclass();
        while (c != null && c != Object.class) {
            final Boolean ret = immutables.get(c);
            if (ret != null && ret != false) {
                immutables.put(clz, Boolean.TRUE);
                return true;
            }
            c = c.getSuperclass();
        }
        immutables.put(clz, Boolean.FALSE);
        return false;
    }

    /**
     * PLEASE DONT CALL THIS METHOD The only reason for been public is because
     * custom IFastCloner's must invoke it
     */
    @SuppressWarnings("unchecked")
    public <T> T cloneInternal(final T o, final Map<Object, Object> clones)
            throws IllegalAccessException {
        if (o == null)
            return null;
        if (o == this)
            return null;
        if (ignoredInstances.containsKey(o))
            return o;
        final Class<T> clz = (Class<T>) o.getClass();
        if (clz.isEnum())
            return o;
        // skip cloning ignored classes
        if (nullInstead.contains(clz))
            return null;
        if (isIgnored(clz))
            return o;
        for (final Class<?> iClz : ignoredInstanceOf) {
            if (iClz.isAssignableFrom(clz))
                return o;
        }
        if (isImmutable(clz))
            return o;
        if (o instanceof IFreezable) {
            final IFreezable f = (IFreezable) o;
            if (f.isFrozen())
                return o;
        }
        final Object clonedPreviously = clones != null ? clones.get(o) : null;
        if (clonedPreviously != null)
            return (T) clonedPreviously;

        final Object fastClone = fastClone(o, clones);
        if (fastClone != null) {
            if (clones != null) {
                clones.put(o, fastClone);
            }
            return (T) fastClone;
        }
        long start = 0L;
        if (dumpCloneStatistics)
            start = System.currentTimeMillis();
        if (dumpClonedClasses) {
            System.out.println("clone>" + clz);
        }
        if (clz.isArray()) {
            final int length = Array.getLength(o);
            final T newInstance = (T) Array.newInstance(clz.getComponentType(), length);
            if (clones != null) {
                clones.put(o, newInstance);
            }
            for (int i = 0; i < length; i++) {
                final Object v = Array.get(o, i);
                final Object clone = clones != null ? cloneInternal(v, clones) : v;
                try {
                    Array.set(newInstance, i, clone);
                } catch (Exception ex) {
                    Array.set(newInstance, i, v);
                }
            }
            return newInstance;
        }

        final T newInstance = newInstance(clz);
        if (clones != null) {
            clones.put(o, newInstance);
        }
        final List<Field> fields = allFields(clz);
        for (final Field field : fields) {
            final int modifiers = field.getModifiers();
            if (!Modifier.isStatic(modifiers)) {
                if (nullTransient && Modifier.isTransient(modifiers)) {
                    // request by Jonathan : transient fields can be null-ed
                    final Class<?> type = field.getType();
                    if (!type.isPrimitive()) {
                        field.set(newInstance, null);
                    }
                } else {
                    final Object fieldObject = field.get(o);
                    final boolean shouldClone = (cloneSynthetics || (!cloneSynthetics && !field
                            .isSynthetic()))
                            && (cloneAnonymousParent || ((!cloneAnonymousParent && !isAnonymousParent(field))));
                    final Object fieldObjectClone = clones != null ? (shouldClone ? cloneInternal(
                            fieldObject, clones) : fieldObject) : fieldObject;
                    try {
                        field.set(newInstance, fieldObjectClone);
                    } catch (Exception ex) {
                        // ex.printStackTrace();
                        // System.exit(-1);
                    }
                    if (dumpClonedClasses && fieldObjectClone != fieldObject) {
                        System.out.println("cloned field>" + field + "  -- of class "
                                + o.getClass());
                    }
                }
            }
        }
        if (dumpCloneStatistics) {
            long end = System.currentTimeMillis();

            if (!timePerClass.containsKey(clz))
                timePerClass.put(clz, 0L);
            timePerClass.put(clz, timePerClass.get(clz) + (end - start));
        }
        return newInstance;
    }

    public static void printStatistics() {
        for (Class<?> c : timePerClass.keySet()) {
            System.out.println(((timePerClass.get(c) / 1000) + "\t" + c.getName()));
        }
    }

    private boolean isAnonymousParent(final Field field) {
        return "this$0".equals(field.getName());
    }

    /**
     * copies all properties from src to dest. Src and dest can be of different
     * class, provided they contain same field names/types
     * 
     * @param src the source object
     * @param dest the destination object which must contain as minimum all the
     *            fields of src
     */
    public <T, E extends T> void copyPropertiesOfInheritedClass(final T src, final E dest) {
        if (src == null)
            throw new IllegalArgumentException("src can't be null");
        if (dest == null)
            throw new IllegalArgumentException("dest can't be null");
        final Class<? extends Object> srcClz = src.getClass();
        final Class<? extends Object> destClz = dest.getClass();
        if (srcClz.isArray()) {
            if (!destClz.isArray())
                throw new IllegalArgumentException("can't copy from array to non-array class "
                        + destClz);
            final int length = Array.getLength(src);
            for (int i = 0; i < length; i++) {
                final Object v = Array.get(src, i);
                Array.set(dest, i, v);
            }
            return;
        }
        final List<Field> fields = allFields(srcClz);
        final List<Field> destFields = allFields(dest.getClass());
        for (final Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers())) {
                try {
                    final Object fieldObject = field.get(src);
                    field.setAccessible(true);
                    if (destFields.contains(field)) {
                        field.set(dest, fieldObject);
                    }
                } catch (final IllegalArgumentException e) {
                    throw new RuntimeException(e);
                } catch (final IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * reflection utils
     */
    private void addAll(final List<Field> l, final Field[] fields) {
        for (final Field field : fields) {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            l.add(field);
        }
    }

    /**
     * reflection utils, override this to choose which fields to clone
     */
    protected List<Field> allFields(final Class<?> c) {
        List<Field> l = fieldsCache.get(c);
        if (l == null) {
            l = new LinkedList<Field>();
            final Field[] fields = c.getDeclaredFields();
            addAll(l, fields);
            Class<?> sc = c;
            while ((sc = sc.getSuperclass()) != Object.class && sc != null) {
                addAll(l, sc.getDeclaredFields());
            }
            fieldsCache.putIfAbsent(c, l);
        }
        return l;
    }

    public boolean isDumpClonedClasses() {
        return dumpClonedClasses;
    }

    /**
     * will println() all cloned classes. Useful for debugging only.
     * 
     * @param dumpClonedClasses true to enable printing all cloned classes
     */
    public void setDumpClonedClasses(final boolean dumpClonedClasses) {
        this.dumpClonedClasses = dumpClonedClasses;
    }

    public boolean isCloningEnabled() {
        return cloningEnabled;
    }

    public void setCloningEnabled(final boolean cloningEnabled) {
        this.cloningEnabled = cloningEnabled;
    }

    /**
     * if false, anonymous classes parent class won't be cloned. Default is true
     */
    public void setCloneAnonymousParent(final boolean cloneAnonymousParent) {
        this.cloneAnonymousParent = cloneAnonymousParent;
    }

    public boolean isCloneAnonymousParent() {
        return cloneAnonymousParent;
    }

    public void setDumpCloneStatistics(boolean dumpCloneStatistics) {
        this.dumpCloneStatistics = dumpCloneStatistics;
    }

    @Override
    protected void finalize() throws Throwable {
        if (dumpCloneStatistics)
            Cloner.printStatistics();
        super.finalize();
    }
}
