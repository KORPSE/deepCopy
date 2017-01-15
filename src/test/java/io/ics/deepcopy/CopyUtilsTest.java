package io.ics.deepcopy;

import junit.framework.TestCase;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;

public class CopyUtilsTest extends TestCase {

    public void testBasic() {
        ArrayList<String> test = new ArrayList<>();
        test.add("one");
        test.add("two");
        test.add("three");

        ArrayList copy = CopyUtils.copy(test);
        assertTrue(test != copy);
        assertEquals(test, copy);
    }

    public void testPrimitiveArray() {
        int[] array = new int[] { 1, 2, 3 };
        int[] copy = CopyUtils.copy(array);
        assertTrue(array != copy);
        assertTrue(Arrays.equals(array, copy));
    }

    public void testObjectArray() {
        LocalDateTime[] array = new LocalDateTime[] {
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1)
        };
        LocalDateTime[] copy = CopyUtils.copy(array);
        assertTrue(Arrays.equals(array, copy));
    }

    public void testNested() {
        ArrayList<Object> test = new ArrayList<>();
        test.add("one");
        test.add("two");
        test.add("three");

        ArrayList<Object> test2 = new ArrayList<>();
        test2.add(1);
        test2.add(2f);
        test2.add(3d);
        test2.add((byte) 4);
        test2.add((short) 5);
        test.add(test2);

        ArrayList copy = CopyUtils.copy(test);
        assertTrue(test != copy);
        assertTrue(test.get(3) != copy.get(3));
        assertEquals(test, copy);
    }

    public void testReferences() {
        ArrayList<Object> test = new ArrayList<>();
        test.add("one");
        test.add("two");
        test.add("three");

        ArrayList<Object> test2 = new ArrayList<>();
        test2.add(1);
        test2.add(2f);
        test2.add(3d);
        test2.add((byte) 4);
        test2.add((short) 5);
        test.add(test2);
        test.add(test2);
        test.add(test2);

        ArrayList copy = CopyUtils.copy(test);
        assertTrue(test != copy);
        assertEquals(test, copy);
        assertTrue(test.get(3) != copy.get(4));
        assertTrue(copy.get(3) == copy.get(4));
        assertTrue(copy.get(3) == copy.get(5));
    }

    public void testCircularReferences() {
        ArrayList<Object> test = new ArrayList<>();
        test.add("one");
        test.add("two");
        test.add("three");
        test.add(test);

        ArrayList copy = CopyUtils.copy(test);
        assertTrue(test != copy);
        assertEquals(copy.get(0), "one");
        assertEquals(copy.get(1), "two");
        assertEquals(copy.get(2), "three");
        assertTrue(copy.get(3) == copy);

        Moo root = new Moo();
        Moo moo1 = new Moo();
        Moo moo2 = new Moo();
        Moo moo3 = new Moo();
        root.moo = moo1;
        moo1.moo = moo2;
        moo2.moo = moo3;
        moo3.moo = root;
        Moo root2 = CopyUtils.copy(root);
        assertTrue(root != root2);
        assertTrue(root.moo != root2.moo);
        assertTrue(root.moo.moo != root2.moo.moo);
        assertTrue(root.moo.moo.moo != root2.moo.moo.moo);
        assertTrue(root.moo.moo.moo.moo != root2.moo.moo.moo.moo);
        assertTrue(root.moo.moo.moo.moo == root);
        assertTrue(root2.moo.moo.moo.moo == root2);
    }

    public void testEnum() {
        EnumSet<Animal> enumSet = EnumSet.allOf(Animal.class);
        EnumSet<Animal> copy = CopyUtils.copy(enumSet);
        assertTrue(enumSet != copy);
        assertTrue(copy.contains(Animal.CAT));
        assertTrue(copy.contains(Animal.DOG));
        assertEquals(enumSet, copy);
    }

    public void testInheritedProperties() {
        Child object = new Child();
        object.parentId = 1;
        object.childId = 2;
        Child copy = CopyUtils.copy(object);
        assertEquals(copy.childId, object.childId);
        assertEquals(copy.parentId, object.parentId);
        assertTrue(object != copy);
    }

    public void testCopyFile() throws IOException {
        String testString = "test";
        File file = File.createTempFile("tmp", "txt");
        file.deleteOnExit();
        File copy = CopyUtils.copy(file);
        Writer writer = new FileWriter(copy);
        writer.write(testString);
        writer.close();
        BufferedReader reader = new BufferedReader(new FileReader(copy));
        String actual = reader.readLine();
        assertEquals(actual, testString);
    }

    public void testCopyOpenedWriter() throws IOException {
        String[] strings = new String[] { "test", "test_copy" };
        File file = File.createTempFile("tmp", "txt");
        file.deleteOnExit();
        Writer writer = new FileWriter(file);
        writer.write(strings[0] + "\n");
        Writer copy = CopyUtils.copy(writer);
        copy.write(strings[1]);
        // close only copy writer because in fact there
        // is the same file descriptor
        copy.close();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String[] actual = reader.lines().toArray(String[]::new);
        assertTrue(Arrays.equals(actual, strings));
    }

    public void testCopyWithStaticProp() {
        WithStaticField src = new WithStaticField();
        WithStaticField.staticFld = 2;
        src.nonstatic = 1;
        WithStaticField copy = CopyUtils.copy(src);
        assertEquals(src.nonstatic, copy.nonstatic);
        assertEquals(src.staticFld, copy.staticFld);
    }

    private static class Parent {
        int parentId;
    }

    private static class Child extends Parent {
        int childId;
    }

    private static class WithStaticField {
        static int staticFld;
        int nonstatic;
    }

    private enum Animal {
        DOG, CAT
    }

    private static class Moo {
        Moo moo;
    }

}
