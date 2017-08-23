package softtech.hong.hce.criterion; 

public final class ExtraProjections
{ 
    public static MultipleCountProjection countMultipleDistinct(String propertyNames) {
        return new MultipleCountProjection(propertyNames).setDistinct();
    }
}