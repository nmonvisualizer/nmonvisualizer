package com.ibm.nmon.data.definition;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.matcher.FieldMatcher;
import com.ibm.nmon.data.matcher.HostMatcher;
import com.ibm.nmon.data.matcher.TypeMatcher;
import com.ibm.nmon.data.transform.name.NameTransformer;

import com.ibm.nmon.analysis.Statistic;

/**
 * <p>
 * Standard {@link DataDefintion} that uses various <code>matcher</code> classes to define the data
 * this class will match. Data is renamed using various {@link NameTransformer} classes.
 * </p>
 * 
 * <p>
 * By default, this definition matches all hosts, types and fields; it does no renaming.
 * </p>
 * 
 * @see HostMatcher
 * @see TypeMatcher
 * @see FieldMatcher
 * @see NameTransformer
 */
public final class DefaultDataDefinition extends DataDefinition {
    /**
     * Use this key to specify the <code>NameTransformer</code> to use by default in the rename
     * methods.
     */
    public static final String DEFAULT_NAME_TRANSFORMER_KEY = "$ALL";

    private Map<String, NameTransformer> hostTransformers;
    private Map<String, NameTransformer> typeTransformers;
    private Map<String, NameTransformer> fieldTransformers;

    private final HostMatcher hostMatcher;
    private final TypeMatcher typeMatcher;
    private final FieldMatcher fieldMatcher;

    public DefaultDataDefinition(HostMatcher hostMatcher, TypeMatcher typeMatcher, FieldMatcher fieldMatcher,
            Statistic stat, boolean useSecondaryYAxis) {
        super(stat, useSecondaryYAxis);

        this.hostMatcher = hostMatcher == null ? HostMatcher.ALL : hostMatcher;
        this.typeMatcher = typeMatcher == null ? TypeMatcher.ALL : typeMatcher;
        this.fieldMatcher = fieldMatcher == null ? FieldMatcher.ALL : fieldMatcher;
    }

    public void addHostnameTransformer(String hostname, NameTransformer transformer) {
        if (transformer != null) {
            if (hostTransformers == null) {
                hostTransformers = new java.util.HashMap<String, NameTransformer>(2);
            }

            hostTransformers.put(hostname, transformer);
        }
    }

    public void addTypeTransformer(String typeId, NameTransformer transformer) {
        if (transformer != null) {
            if (typeTransformers == null) {
                typeTransformers = new java.util.HashMap<String, NameTransformer>(2);
            }

            typeTransformers.put(typeId, transformer);
        }
    }

    public void addFieldTransformer(String field, NameTransformer transformer) {
        if (transformer != null) {
            if (fieldTransformers == null) {
                fieldTransformers = new java.util.HashMap<String, NameTransformer>(2);
            }

            fieldTransformers.put(field, transformer);
        }
    }

    public HostMatcher getHostMatcher() {
        return hostMatcher;
    }

    public TypeMatcher getTypeMatcher() {
        return typeMatcher;
    }

    public FieldMatcher getFieldMatcher() {
        return fieldMatcher;
    }

    @Override
    public boolean matchesHost(DataSet data) {
        return hostMatcher.matchesHost(data);
    }

    @Override
    public List<DataSet> getMatchingHosts(Collection<DataSet> toMatch) {
        return hostMatcher.getMatchingHosts(toMatch);
    }

    @Override
    public List<DataType> getMatchingTypes(DataSet data) {
        return typeMatcher.getMatchingTypes(data);
    }

    @Override
    public List<String> getMatchingFields(DataType type) {
        return fieldMatcher.getMatchingFields(type);
    }

    @Override
    public String renameHost(DataSet data) {
        String hostname = data.getHostname();

        if (hostTransformers != null) {
            NameTransformer transformer = hostTransformers.get(hostname);

            if (transformer != null) {
                return transformer.transform(hostname);
            }
            else {
                transformer = hostTransformers.get(DEFAULT_NAME_TRANSFORMER_KEY);

                if (transformer != null) {
                    return transformer.transform(hostname);
                }
                else {
                    return hostname;
                }
            }
        }
        else {
            return hostname;
        }
    }

    @Override
    public String renameType(DataType type) {
        String typeId = super.renameType(type);

        if (typeTransformers != null) {
            NameTransformer transformer = typeTransformers.get(typeId);

            if (transformer != null) {
                return transformer.transform(typeId);
            }
            else {
                transformer = typeTransformers.get(DEFAULT_NAME_TRANSFORMER_KEY);

                if (transformer != null) {
                    return transformer.transform(typeId);
                }
                else {
                    return typeId;
                }
            }
        }
        else {
            return typeId;
        }
    }

    public String renameField(String field) {
        field = super.renameField(field);

        if (fieldTransformers != null) {
            NameTransformer transformer = fieldTransformers.get(field);

            if (transformer != null) {
                return transformer.transform(field);
            }
            else {
                transformer = fieldTransformers.get(DEFAULT_NAME_TRANSFORMER_KEY);

                if (transformer != null) {
                    return transformer.transform(field);
                }
                else {
                    return field;
                }
            }
        }
        else {
            return field;
        }
    }

    public DefaultDataDefinition withNewHosts(HostMatcher matcher) {
        if (matcher != null) {
            if (hostMatcher.equals(matcher)) {
                return this;
            }
            else {
                return new DefaultDataDefinition(matcher, this.typeMatcher, this.fieldMatcher, this.getStatistic(),
                        this.usesSecondaryYAxis());
            }
        }
        else {
            return this;
        }
    }

    public DefaultDataDefinition withNewTypes(TypeMatcher matcher) {
        if (matcher != null) {
            if (typeMatcher.equals(matcher)) {
                return this;
            }
            else {
                return new DefaultDataDefinition(this.hostMatcher, matcher, this.fieldMatcher, this.getStatistic(),
                        this.usesSecondaryYAxis());
            }
        }
        else {
            return this;
        }
    }

    public DefaultDataDefinition withNewFields(FieldMatcher matcher) {
        if (matcher != null) {
            if (fieldMatcher.equals(matcher)) {
                return this;
            }
            else {
                return new DefaultDataDefinition(this.hostMatcher, this.typeMatcher, matcher, this.getStatistic(),
                        this.usesSecondaryYAxis());
            }
        }
        else {
            return this;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(256);

        builder.append('{');

        builder.append("host: ");
        builder.append("match=");
        builder.append(hostMatcher);
        if (hostTransformers != null) {
            builder.append(", ");
            builder.append("transforms=");
            builder.append(hostTransformers);
        }
        builder.append("; ");

        builder.append("type: ");
        builder.append("match=");
        builder.append(typeMatcher);
        if (typeTransformers != null) {
            builder.append(", ");
            builder.append("transforms=");
            builder.append(typeTransformers);
        }
        builder.append("; ");

        builder.append("field: ");
        builder.append("match=");
        builder.append(fieldMatcher);
        if (fieldTransformers != null) {
            builder.append(", ");
            builder.append("transforms=");
            builder.append(fieldTransformers);
        }

        if (!Statistic.AVERAGE.equals(getStatistic())) {
            builder.append("; ");
            builder.append("stat=");
            builder.append(getStatistic());
        }

        builder.append('}');

        return builder.toString();
    }
}
