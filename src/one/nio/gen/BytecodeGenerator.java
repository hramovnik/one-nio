package one.nio.gen;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class BytecodeGenerator extends ClassLoader implements Opcodes {
    private static final Log log = LogFactory.getLog(BytecodeGenerator.class);
    private static final String DUMP_PATH = System.getProperty("one.nio.gen.dump");

    public BytecodeGenerator() {
        super(BytecodeGenerator.class.getClassLoader());
    }

    public Class<?> defineClass(byte[] classData) {
        Class<?> result = super.defineClass(null, classData, 0, classData.length, null);
        if (DUMP_PATH != null) {
            dumpClass(classData, result.getSimpleName());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T> T instantiate(byte[] classData, Class<T> iface) {
        try {
            return (T) defineClass(classData).newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot instantiate class", e);
        }
    }

    public void dumpClass(byte[] classData, String className) {
        try {
            File f = new File(DUMP_PATH, className + ".class");
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(classData);
            fos.close();
        } catch (IOException e) {
            log.error("Could not dump " + className, e);
        }
    }

    public static void emitGetField(MethodVisitor mv, Field f) {
        int opcode = (f.getModifiers() & Modifier.STATIC) != 0 ? GETSTATIC : GETFIELD;
        String holder = Type.getInternalName(f.getDeclaringClass());
        String name = f.getName();
        String sig = Type.getDescriptor(f.getType());
        mv.visitFieldInsn(opcode, holder, name, sig);
    }

    public static void emitPutField(MethodVisitor mv, Field f) {
        int opcode = (f.getModifiers() & Modifier.STATIC) != 0 ? PUTSTATIC : PUTFIELD;
        String holder = Type.getInternalName(f.getDeclaringClass());
        String name = f.getName();
        String sig = Type.getDescriptor(f.getType());
        mv.visitFieldInsn(opcode, holder, name, sig);
    }

    public static void emitInvoke(MethodVisitor mv, Method m) {
        int opcode;
        if ((m.getModifiers() & Modifier.STATIC) != 0) {
            opcode = INVOKESTATIC;
        } else if ((m.getModifiers() & Modifier.PRIVATE) != 0) {
            opcode = INVOKESPECIAL;
        } else if (m.getDeclaringClass().isInterface()) {
            opcode = INVOKEINTERFACE;
        } else {
            opcode = INVOKEVIRTUAL;
        }

        String holder = Type.getInternalName(m.getDeclaringClass());
        String name = m.getName();
        String sig = Type.getMethodDescriptor(m);
        mv.visitMethodInsn(opcode, holder, name, sig);
    }

    public static void emitInt(MethodVisitor mv, int c) {
        if (c >= -1 && c <= 5) {
            mv.visitInsn(ICONST_0 + c);
        } else if (c >= Byte.MIN_VALUE && c <= Byte.MAX_VALUE) {
            mv.visitIntInsn(BIPUSH, c);
        } else if (c >= Short.MIN_VALUE && c <= Short.MAX_VALUE) {
            mv.visitIntInsn(SIPUSH, c);
        } else {
            mv.visitLdcInsn(c);
        }
    }

    public static void emitLong(MethodVisitor mv, long c) {
        if (c >= 0 && c <= 1) {
            mv.visitInsn(LCONST_0 + (int) c);
        } else {
            mv.visitLdcInsn(c);
        }
    }

    public static void emitFloat(MethodVisitor mv, float c) {
        if (c == 0.0f) {
            mv.visitInsn(FCONST_0);
        } else if (c == 1.0f) {
            mv.visitInsn(FCONST_1);
        } else if (c == 2.0f) {
            mv.visitInsn(FCONST_2);
        } else {
            mv.visitLdcInsn(c);
        }
    }

    public static void emitDouble(MethodVisitor mv, double c) {
        if (c == 0.0) {
            mv.visitInsn(DCONST_0);
        } else if (c == 1.0) {
            mv.visitInsn(DCONST_1);
        } else {
            mv.visitLdcInsn(c);
        }
    }
}
