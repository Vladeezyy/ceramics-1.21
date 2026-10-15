package knightminer.ceramics.datagen;

import net.minecraft.data.tags.TagAppender;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.util.function.Function;

/** Tag appender taking the objects themselves, since 26.2 the vanilla appender takes registry keys */
final class ObjectTagAppender<T> {
  private final TagAppender<T> parent;
  private final Function<T, ResourceKey<T>> key;

  ObjectTagAppender(TagAppender<T> parent, Function<T, ResourceKey<T>> key) {
    this.parent = parent;
    this.key = key;
  }

  @SafeVarargs
  final ObjectTagAppender<T> add(T... values) {
    for (T value : values) {
      parent.add(key.apply(value));
    }
    return this;
  }

  ObjectTagAppender<T> addTag(TagKey<T> tag) {
    parent.addTag(tag);
    return this;
  }

  ObjectTagAppender<T> addOptionalTag(TagKey<T> tag) {
    parent.addOptionalTag(tag);
    return this;
  }
}
