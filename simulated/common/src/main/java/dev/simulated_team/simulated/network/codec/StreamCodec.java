package dev.simulated_team.simulated.network.codec;

import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import com.mojang.datafixers.util.Function6;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Java 17/1.20.1 counterpart of the 1.21 StreamCodec surface used by the
 * project. Composite codecs retain the upstream field order in both directions.
 */
public interface StreamCodec<B, V> {
    V decode(B buffer);

    void encode(B buffer, V value);

    static <B, V> StreamCodec<B, V> of(BiConsumer<B, V> encoder, Function<B, V> decoder) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buffer) {
                return decoder.apply(buffer);
            }

            @Override
            public void encode(B buffer, V value) {
                encoder.accept(buffer, value);
            }
        };
    }

    static <B, V> StreamCodec<B, V> ofMember(BiConsumer<V, B> encoder, Function<B, V> decoder) {
        return of((buffer, value) -> encoder.accept(value, buffer), decoder);
    }

    static <B, V> StreamCodec<B, V> unit(V expected) {
        return of((buffer, value) -> {
            if (!expected.equals(value)) {
                throw new IllegalStateException("Unexpected unit value: " + value);
            }
        }, buffer -> expected);
    }

    default <O> StreamCodec<B, O> apply(Function<StreamCodec<B, V>, StreamCodec<B, O>> transformer) {
        return transformer.apply(this);
    }

    default <O> StreamCodec<B, O> map(Function<? super V, ? extends O> mapper,
                                       Function<? super O, ? extends V> unmapper) {
        return of((buffer, value) -> this.encode(buffer, unmapper.apply(value)),
                buffer -> mapper.apply(this.decode(buffer)));
    }

    static <B, A, V> StreamCodec<B, V> composite(
            StreamCodec<? super B, A> first, Function<V, A> firstGetter,
            Function<A, V> constructor) {
        return of((buffer, value) -> first.encode(buffer, firstGetter.apply(value)),
                buffer -> constructor.apply(first.decode(buffer)));
    }

    static <B, A, C, V> StreamCodec<B, V> composite(
            StreamCodec<? super B, A> first, Function<V, A> firstGetter,
            StreamCodec<? super B, C> second, Function<V, C> secondGetter,
            BiFunction<A, C, V> constructor) {
        return of((buffer, value) -> {
            first.encode(buffer, firstGetter.apply(value));
            second.encode(buffer, secondGetter.apply(value));
        }, buffer -> constructor.apply(first.decode(buffer), second.decode(buffer)));
    }

    static <B, A, C, D, V> StreamCodec<B, V> composite(
            StreamCodec<? super B, A> first, Function<V, A> firstGetter,
            StreamCodec<? super B, C> second, Function<V, C> secondGetter,
            StreamCodec<? super B, D> third, Function<V, D> thirdGetter,
            Function3<A, C, D, V> constructor) {
        return of((buffer, value) -> {
            first.encode(buffer, firstGetter.apply(value));
            second.encode(buffer, secondGetter.apply(value));
            third.encode(buffer, thirdGetter.apply(value));
        }, buffer -> constructor.apply(first.decode(buffer), second.decode(buffer), third.decode(buffer)));
    }

    static <B, A, C, D, E, V> StreamCodec<B, V> composite(
            StreamCodec<? super B, A> first, Function<V, A> firstGetter,
            StreamCodec<? super B, C> second, Function<V, C> secondGetter,
            StreamCodec<? super B, D> third, Function<V, D> thirdGetter,
            StreamCodec<? super B, E> fourth, Function<V, E> fourthGetter,
            Function4<A, C, D, E, V> constructor) {
        return of((buffer, value) -> {
            first.encode(buffer, firstGetter.apply(value));
            second.encode(buffer, secondGetter.apply(value));
            third.encode(buffer, thirdGetter.apply(value));
            fourth.encode(buffer, fourthGetter.apply(value));
        }, buffer -> constructor.apply(first.decode(buffer), second.decode(buffer), third.decode(buffer),
                fourth.decode(buffer)));
    }

    static <B, A, C, D, E, F, V> StreamCodec<B, V> composite(
            StreamCodec<? super B, A> first, Function<V, A> firstGetter,
            StreamCodec<? super B, C> second, Function<V, C> secondGetter,
            StreamCodec<? super B, D> third, Function<V, D> thirdGetter,
            StreamCodec<? super B, E> fourth, Function<V, E> fourthGetter,
            StreamCodec<? super B, F> fifth, Function<V, F> fifthGetter,
            Function5<A, C, D, E, F, V> constructor) {
        return of((buffer, value) -> {
            first.encode(buffer, firstGetter.apply(value));
            second.encode(buffer, secondGetter.apply(value));
            third.encode(buffer, thirdGetter.apply(value));
            fourth.encode(buffer, fourthGetter.apply(value));
            fifth.encode(buffer, fifthGetter.apply(value));
        }, buffer -> constructor.apply(first.decode(buffer), second.decode(buffer), third.decode(buffer),
                fourth.decode(buffer), fifth.decode(buffer)));
    }

    static <B, A, C, D, E, F, G, V> StreamCodec<B, V> composite(
            StreamCodec<? super B, A> first, Function<V, A> firstGetter,
            StreamCodec<? super B, C> second, Function<V, C> secondGetter,
            StreamCodec<? super B, D> third, Function<V, D> thirdGetter,
            StreamCodec<? super B, E> fourth, Function<V, E> fourthGetter,
            StreamCodec<? super B, F> fifth, Function<V, F> fifthGetter,
            StreamCodec<? super B, G> sixth, Function<V, G> sixthGetter,
            Function6<A, C, D, E, F, G, V> constructor) {
        return of((buffer, value) -> {
            first.encode(buffer, firstGetter.apply(value));
            second.encode(buffer, secondGetter.apply(value));
            third.encode(buffer, thirdGetter.apply(value));
            fourth.encode(buffer, fourthGetter.apply(value));
            fifth.encode(buffer, fifthGetter.apply(value));
            sixth.encode(buffer, sixthGetter.apply(value));
        }, buffer -> constructor.apply(first.decode(buffer), second.decode(buffer), third.decode(buffer),
                fourth.decode(buffer), fifth.decode(buffer), sixth.decode(buffer)));
    }
}
