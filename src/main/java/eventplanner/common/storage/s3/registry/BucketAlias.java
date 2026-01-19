package eventplanner.common.storage.s3.registry;

public enum BucketAlias {
    EVENT("event"),
    USER("user");

    private final String alias;

    BucketAlias(String alias) {
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }

    public String resolve(BucketRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("BucketRegistry is required");
        }
        return registry.resolve(alias);
    }

    @Override
    public String toString() {
        return alias;
    }
}
