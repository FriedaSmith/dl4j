package cn.centipede.numpy;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class Numpy extends NumpyBase{
    public static final int ALL = -999_999_999;
    public static final int newaxis = -999_999_998; // np.newaxis

    // tricky impl as python numpy
    public class np extends Numpy{}
    public class random extends Random{}

    /**
     * Get a real array from NDArray
     * NDArray uses a data buffer not a muti-dim array
     */
    public static Object getArray(NDArray array) {
        Object real = getArrayData(array);
        int[] dim = array.dimens();
        return ArrayHelper.struct(real, dim);
    }

    /**
     * NDArray maybe a sparse array with idata
     * share data with other var
     * @param array
     * @return standalone data
     */
    public static Object getArrayData(NDArray array) {
        Object data = array.data();
        int[] index = array.dataIndex();

        if (data instanceof int[]) {
            return IntStream.of(index).map(i-> (int) Array.get(data, i)).toArray();
        } else if (data instanceof Integer) {
            return new int[]{(int)data};
        } else if (data instanceof Double) {
            return new double[]{(double)data};
        } else {
            return IntStream.of(index).mapToDouble(i-> (double) Array.get(data, i)).toArray();
        }
    }

    public static NDArray zeros(int... dimens) {
        return zeros(dimens, double.class);
    }

    public static NDArray zeros(int[] dimens, Class<?> dtype) {
        int size = IntStream.of(dimens).reduce(1, (a, b) -> a * b);
        Object array = Array.newInstance(dtype, size);
        for (int i = 0; i < size; i++) {
            Array.set(array, i, 0);
        }
        return new NDArray(array, dimens);
    }

    public static NDArray ones(int... dimens) {
        return ones(dimens, double.class);
    }

    public static NDArray ones(int[] dimens, Class<?> dtype) {
        int size = IntStream.of(dimens).reduce(1, (a, b) -> a * b);
        Object array = Array.newInstance(dtype, size);
        for (int i = 0; i < size; i++) {
            Array.set(array, i, 1);
        }
        return new NDArray(array, dimens);
    }

    /**
     * arange(9) = [0,1,2,3,4,5,6,7,8]
     * @param top = 9
     * @param dtype int/double
     * @return
     */
    public static NDArray arange(int top, Class<?> dtype) {
        Object array = Array.newInstance(dtype, top);
        for (int i = 0; i < top; i++) {
            Array.set(array, i, i);
        }

        return new NDArray(array, top);
    }

    public static NDArray linspace(double start, double top, int count) {
        double[] array = new double[count];
        double distance = top - start;
        double step = distance/(count-1);

        for (int i = 0; i < count; i++) {
            array[i] = start + step*i;
        }

        return new NDArray(array);
    }

    /**
     * data: NDArray, number, int[][][], double[][][]
     * genertic
     * convenrt to int[] or double[]
     */
    private static Object dat2Array(Object data) {
        Object array;

        if (data instanceof NDArray) {
            array = getArrayData((NDArray) data);
        } else if (data.getClass().isArray()) {
            array = ArrayHelper.flatten(data)[1];
        } else {
            array = data instanceof Integer?new int[]{(int) data}:new double[]{(double)data};
        }
       return array;
    }

    /**
     * support double & int
     * @param src
     * @param data
     * @return
     */
    public static NDArray add(NDArray src, Object dat) {
        Object srcData   = getArrayData(src);
        Object datArray  = dat2Array(dat);

        Object ret = doOp(srcData, datArray, Operator::add);
        return new NDArray(ret,  src.dimens());
    }

    public static NDArray subtract(NDArray src, Object dat) {
        Object srcData   = getArrayData(src);
        Object datArray  = dat2Array(dat);

        Object ret = doOp(srcData, datArray, Operator::subtract);
        return new NDArray(ret,  src.dimens());
    }

    public static NDArray multiply(NDArray src, Object dat) {
        Object srcData   = getArrayData(src);
        Object datArray  = dat2Array(dat);

        Object ret = doOp(srcData, datArray, Operator::multiply);
        return new NDArray(ret,  src.dimens());
    }

    public static NDArray divide(NDArray src, Object dat) {
        Object srcData   = getArrayData(src);
        Object datArray  = dat2Array(dat);

        if (src.isInt()) {
            srcData = IntStream.of((int[])srcData).asDoubleStream().toArray();
        }

        if (datArray instanceof int[]) {
            datArray = IntStream.of((int[])datArray).asDoubleStream().toArray();
        }

        Object ret = doOp(srcData, datArray, Operator::divide);
        return new NDArray(ret,  src.dimens());
    }

    public static NDArray exp(NDArray src) {
        Object srcData   = getArrayData(src);

        if (src.isInt()) {
            srcData = IntStream.of((int[])srcData).asDoubleStream().toArray();
        }

        Object ret = doOp(srcData, Operator::exp);
        return new NDArray(ret,  src.dimens());
    }

    public static NDArray log(NDArray src) {
        Object srcData   = getArrayData(src);

        if (src.isInt()) {
            srcData = IntStream.of((int[])srcData).asDoubleStream().toArray();
        }

        Object ret = doOp(srcData, Operator::log);
        return new NDArray(ret,  src.dimens());
    }

    public static NDArray abs(NDArray src) {
        Object srcData   = getArrayData(src);

        if (src.isInt()) {
            srcData = IntStream.of((int[])srcData).map(n->n>=0?n:-n).toArray();
        } else {
            srcData = DoubleStream.of((double[])srcData).map(n->n>=0?n:-n).toArray();
        }
        return new NDArray(srcData,  src.dimens());
    }

    /**
     * @param src
     * @return average of dat array
     */
    public static double mean(NDArray src) {
        Object srcData   = getArrayData(src);

        if (src.isInt()) {
            return IntStream.of((int[])srcData).average().getAsDouble();
        } else {
            return DoubleStream.of((double[])srcData).average().getAsDouble();
        }
    }

    /**
     * @param src
     * @return 1/src
     */
    public static NDArray reciprocal(NDArray src) {
        Object srcData   = getArrayData(src);

        if (src.isInt()) {
            srcData = IntStream.of((int[])srcData).asDoubleStream().toArray();
        }

        Object ret = doOp(srcData, Operator::reciprocal);
        return new NDArray(ret,  src.dimens());
    }

    /**
     * Now only support 1-2 dimens
     * @param a number/vector/matrix
     * @param b number/vector/matrix
     * @return NDArray
     */
    public static NDArray dot(NDArray a, NDArray b) {
        int[] aDim = a.dimens(), bDim = b.dimens();
        Object aData = a.data(), bData = b.data();

        /** a is number */
        if (aDim.length == 0) {
            if (aData instanceof Integer)return dot(b, (int)aData);
            else return dot(b, (double)aData);
        }

        /** b is number */
        if (bDim.length == 0) {
            if (bData instanceof Integer)return dot(a, (int)bData);
            else return dot(a, (double)bData);
        }

        /** a is array but not a vector */
        if (aDim.length == 1) {
            aDim = new int[]{1, aDim[0]};
        }

        int[] cDim;

        /** b is array but not a vector */
        if (bDim.length == 1) {
            bDim = new int[]{bDim[0], 1};
            cDim = new int[]{aDim[0]};
        } else {
            cDim = new int[]{aDim[0], bDim[1]};
        }

        aData = getArrayData(a);
        bData = getArrayData(b);

        /** normal dot operation */
        Object cData;
        if (a.isInt() && b.isInt()) {
            cData = dotInt((int[])aData, aDim, (int[])bData, bDim);
        } else if (a.isInt()) {
            double[] aDataNew = IntStream.of((int[])aData).asDoubleStream().toArray();
            cData = dotDouble(aDataNew, aDim, (double[])bData, bDim);
        } else if (b.isInt()) {
            double[] bDataNew = IntStream.of((int[])bData).asDoubleStream().toArray();
            cData = dotDouble((double[])aData, aDim, (double[])bDataNew, bDim);
        } else {
            cData = dotDouble((double[])aData, aDim, (double[])bData, bDim);
        }
        return new NDArray(cData, cDim);
    }

    public static NDArray dot(NDArray a, int b) {
        Object data = getArrayData(a);
        if (data instanceof int[]) {
            data = IntStream.of((int[])data).map(n->n*b).toArray();
        } else {
            data = DoubleStream.of((double[]) data).map(n->n*b).toArray();
        }
        return Numpy.array(data, a.dimens());
    }

    public static NDArray dot(NDArray a, double b) {
        Object data = getArrayData(a);
        if (data instanceof int[]) {
            data = IntStream.of((int[])data).mapToDouble(n->n*b).toArray();
        } else {
            data = DoubleStream.of((double[]) data).map(n->n*b).toArray();
        }
        return Numpy.array(data, a.dimens());
    }

    /**
     * src & dst share the same data buffer,
     * but own idata separately
     * @param idataSrc src data index
     * @param dimens dimens array
     * @return transpose data index
     */
    public static int[] transpose(int[] idataSrc, int[] dimens) {
        int[] index = new int[dimens.length];
        int[] idataRet = new int[idataSrc.length];
        int[][] dimensRet = new int[][]{dimens, ArrayHelper.reverse(dimens)};
        doTranspose(idataSrc, idataRet, index, 0, 0, dimensRet);
        return idataRet;
    }

    public static void set(NDArray array, Object data, int... index) {
        array.set(data, index);
    }

    public static NDArray arange(int top) {
        return arange(top, int.class);
    }

    /**
     * [[2,3,4],[4,5,6]] -> flatten
     * -> obj{[2,3,4,4,5,6], int[] dimens}
     * @param dimens multi-dimens
     * @return NDArray
     */
    public static NDArray array(Object data, int... dimens) {
        if (dimens.length > 0) {
            return new NDArray(data, dimens);
        }

        boolean isArray = data.getClass().isArray();
        if (!isArray) {
            return new NDArray(data);
        }

        Object[] object = ArrayHelper.flatten(data);
        return new NDArray(object[1], (int[]) object[0]);
    }

    /**
     * Matrix is special NDArray
     */
    public static Matrix matrix(Object data) {
        NDArray array = array(data);
        return new Matrix(array);
    }

    /**
     * Now only support axis = 0, two operands !!
     * @param a - 1xn
     * @param b - 1xn
     * @return merge a & b to be a 2xn
     */
    public static NDArray concatenate(NDArray a, NDArray b) {
        return concatenate(a, b, 0);
    }

    public static NDArray concatenate(NDArray a, NDArray b, int axis) {
        if (axis != 0) {
            return mergeArray(a, b, axis);
        }

        int[] adim = a.dimens();
        int[] bdim = b.dimens();

        if (adim.length > 1 && bdim.length == 1) {
            bdim = new int[]{1, bdim[0]};
        }

        int[] ndim;
        if (adim.length == 1) {
            ndim = new int[]{adim[0]+bdim[0]};
        } else {
            ndim = Arrays.copyOf(adim, adim.length);
            ndim[0] = adim[0] + bdim[0];
        }

        Object aArray = getArrayData(a);
        Object bArray = getArrayData(b);
        Object cArray = ArrayHelper.mergeArray(aArray, a.isInt(), bArray, b.isInt());
        return new NDArray(cArray, ndim);
    }

    public static NDArray stack(int axis, NDArray... arrays) {
        return stack(arrays, axis);
    }

    public static NDArray stack(NDArray[] arrays, int axis) {
        int[] dimens_o = arrays[0].dimens();
        if (dimens_o.length == 1 && axis == 1) {
            Stream.of(arrays).forEach(it->it.reshape(ALL, np.newaxis));
        }

        NDArray ret = arrays[0];
        for (int i = 1; i < arrays.length; i++) {
            ret = concatenate(ret, arrays[i], axis);
        }

        int[] dimens_d = new int[dimens_o.length+1];
        for (int i = 0; i < axis; i++) {
            dimens_d[i] = dimens_o[i];
        }

        dimens_d[axis]=arrays.length;
        for (int i = axis; i < dimens_o.length; i++) {
            dimens_d[i+1] = dimens_o[i];
        }
        ret.reshape(dimens_d);
        return ret;
    }

    public static NDArray hstack(NDArray a, NDArray b) {
        return concatenate(a, b, a.dimens().length>1?1:0);
    }

    public static NDArray vstack(NDArray a, NDArray b) {
        NDArray ret = concatenate(a, b, 0);
        int[] dimens = a.dimens();
        return dimens.length>1?ret:ret.reshape(2, dimens[0]);
    }

    /**
     * WANING: axis != 0, a concatenate b
     * internal use only.
     */
    static NDArray mergeArray(NDArray a, NDArray b, int axis) {
        int[] adim = a.dimens();
        NDArray[] rows = new NDArray[adim[0]];
        for (int i = 0; i < adim[0]; i++) {
            rows[i] = concatenate(a.row(i), b.row(i), axis-1);
        }

        NDArray ret = rows[0];

        if (adim[0] > 1) {
            Stream.of(rows).forEach(it->it.reshape(np.newaxis, ALL));
            ret = Stream.of(rows).reduce(Numpy::concatenate).get();
        }

        if (adim[0] == 1 && axis > 0) {
            ret.reshape(np.newaxis, ALL);
        }
        return ret;
    }

    public static double sum(NDArray array) {
        Object dat = getArrayData(array);
        if (array.isInt()) {
            return IntStream.of((int[])dat).sum();
        } else {
            return DoubleStream.of((double[])dat).sum();
        }
    }

    public static int sumInt(NDArray array) {
        Object dat = getArrayData(array);
        return IntStream.of((int[])dat).sum();
    }

    public static boolean compare(NDArray src, NDArray dst) {
        if (!Arrays.equals(src.dimens(), dst.dimens())) {
            return false;
        }
        Object srcData = getArrayData(src);
        Object dstData = getArrayData(dst);
        if (!srcData.getClass().equals(dstData.getClass())) {
            return false;
        }

        if (srcData instanceof int[]) {
            return Arrays.equals((int[])srcData, (int[])dstData);
        } else {
            return Arrays.equals((double[])srcData, (double[])dstData);
        }
    }

    public static boolean same(NDArray src, NDArray dst, double slope) {
        if (!Arrays.equals(src.dimens(), dst.dimens())) {
            return false;
        }
        Object srcData = getArrayData(src);
        Object dstData = getArrayData(dst);
        if (!srcData.getClass().equals(dstData.getClass())) {
            return false;
        }

        if (srcData instanceof int[]) {
            int[] a = (int[])srcData, b = (int[])dstData;
            for (int i = 0; i < a.length; i++) if (Math.abs(a[i] - b[i]) > slope) return false;
            return true;
        } else {
            double[] a = (double[])srcData, b = (double[])dstData;
            for (int i = 0; i < a.length; i++) if (Math.abs(a[i] - b[i]) > slope) return false;
            return true;
        }
    }

    private static double parseDouble(String dat) {
        try {return Double.parseDouble(dat);
        } catch(NumberFormatException e){}
        return 0;
    }

    public static NDArray loadtxt(String fname, final String delimiter) {
        try (Stream<String> lines = Files.lines(Paths.get(fname))) {
            double[][] array = lines.skip(1)
                .map(s -> s.split(delimiter))
                .map(s->Arrays.stream(s).mapToDouble(Numpy::parseDouble).toArray())
                .toArray(double[][]::new);
            return np.array(array);
        } catch(IOException e) {
            System.err.println(e);
        }
        return null;
    }

    private static Object padData(NDArray array, int left, int right) {
        int len = array.size();
        Object data;
        if (array.isInt()) {
            data = new int[left+len+right];
        } else {
            data = new double[left+len+right];
        }
        System.arraycopy(np.getArrayData(array), 0, data, left, len);
        return data;
    }

    private static NDArray _pad(NDArray array, int[] pads) {
        int left = pads[0], right = pads[1];
        int[] dimens = array.dimens();

        if (dimens.length == 1) {
            Object data = padData(array, left, right);
            return np.array(data, dimens[0]+left+right);
        }

        int cell_size = array.row(0).size();
        int[] pad_dimens = Arrays.copyOf(dimens, dimens.length);

        if (left != 0) {
            pad_dimens[0] = left;
            NDArray zeroLeft = np.zeros(new int[]{cell_size*left}, int.class).reshape(pad_dimens);
            array = np.vstack(zeroLeft, array);
        }

        if (right != 0) {
            pad_dimens[0] = right;
            NDArray zeroRight = np.zeros(new int[]{cell_size*right}, int.class).reshape(pad_dimens);
            array = np.vstack(array, zeroRight);
        }

        pads = Arrays.copyOfRange(pads, 2, pads.length);
        NDArray first = _pad(array.row(0), pads);

        for (int i = 1; i < array.dimens()[0]; i++) {
            NDArray row = array.row(i);
            row = _pad(row, pads);
            first = vstack(first, row);
        }
        return first;
    }

    public static NDArray pad(NDArray array, int[][] pads) {
        int[] pads_ = new int[pads.length*2];
        for (int i = 0; i < pads.length; i++) {
            pads_[i*2] = pads[i][0];
            pads_[i*2+1] = pads[i][1];
        }

        int[] dimens = array.dimens();
        NDArray ret = _pad(array, pads_);

        for (int i=0; i < dimens.length; i++) {
            dimens[i] += pads_[i*2];
            dimens[i] += pads_[i*2+1];
        }
        return ret.reshape(dimens);
    }

    public static NDArray pad(NDArray array, int[] pads) {
        int[] dimens = array.dimens();
        int[] pads_ = Arrays.copyOfRange(pads, 0, dimens.length * 2);
        if (pads.length == 1) {
            pads_[1] = pads_[0];
        }

        for (int i = 2; i < pads_.length; i+=2) {
            pads_[i] = pads_[0];
            pads_[i+1] = pads_[1];
        }

        NDArray ret = _pad(array, pads_);
        for (int i=0; i < dimens.length; i++) {
            dimens[i] += pads_[i*2];
            dimens[i] += pads_[i*2+1];
        }
        return ret.reshape(dimens);
    }

    public static NDArray swapaxes(NDArray a, int[] swap) {
        int[] dimens = a.dimens();
        int[][] range = new int[2][];
        range[0] = new int[]{ALL};
        range[1] = new int[1];

        NDArray ret = a.slice(range);
        for (int i = 1; i < dimens[1]; i++) {
            range[1][0] = i;
            ret = np.concatenate(a.slice(range), ret);
        }

        if (a.dimens().length == 2) {
            return ret.reshape(dimens[1], dimens[0]);
        }

        int temp = dimens[0];
        dimens[0] = dimens[1];
        dimens[1] = temp;
        ret.reshape(dimens);

        return ret;
    }

    public static NDArray rot90(NDArray a) {
        int[] dimens = a.dimens();
        int[][] range = new int[2][];
        range[0] = new int[]{ALL};
        range[1] = new int[1];

        NDArray ret = a.slice(range);
        for (int i = 1; i < dimens[1]; i++) {
            range[1][0] = i;
            ret = np.concatenate(a.slice(range), ret);
        }

        if (a.dimens().length == 2) {
            return ret.reshape(dimens[1], dimens[0]);
        }

        int temp = dimens[0];
        dimens[0] = dimens[1];
        dimens[1] = temp;
        ret.reshape(dimens);

        return ret;
    }
}