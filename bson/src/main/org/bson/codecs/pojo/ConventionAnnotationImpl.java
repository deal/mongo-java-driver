/*
 * Copyright 2017 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bson.codecs.pojo;


import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static org.bson.codecs.pojo.PojoBuilderHelper.createPropertyModelBuilder;

final class ConventionAnnotationImpl implements Convention {

    @Override
    public void apply(final ClassModelBuilder<?> classModelBuilder) {
        for (final Annotation annotation : classModelBuilder.getAnnotations()) {
            processClassAnnotation(classModelBuilder, annotation);
        }

        for (PropertyModelBuilder<?> propertyModelBuilder : classModelBuilder.getPropertyModelBuilders()) {
            processPropertyAnnotations(classModelBuilder, propertyModelBuilder);
        }

        processCreatorAnnotation(classModelBuilder);

        cleanPropertyBuilders(classModelBuilder);
    }

    private void processClassAnnotation(final ClassModelBuilder<?> classModelBuilder, final Annotation annotation) {
        if (annotation instanceof BsonDiscriminator) {
            BsonDiscriminator discriminator = (BsonDiscriminator) annotation;
            String key = discriminator.key();
            if (!key.equals("")) {
                classModelBuilder.discriminatorKey(key);
            }

            String name = discriminator.value();
            if (!name.equals("")) {
                classModelBuilder.discriminator(name);
            }
            classModelBuilder.enableDiscriminator(true);
        }
    }

    private void processPropertyAnnotations(final ClassModelBuilder<?> classModelBuilder,
                                            final PropertyModelBuilder<?> propertyModelBuilder) {
        for (Annotation annotation : propertyModelBuilder.getReadAnnotations()) {
            if (annotation instanceof BsonProperty) {
                BsonProperty bsonProperty = (BsonProperty) annotation;
                if (!"".equals(bsonProperty.value())) {
                    propertyModelBuilder.readName(bsonProperty.value());
                }
                propertyModelBuilder.discriminatorEnabled(bsonProperty.useDiscriminator());
            } else if (annotation instanceof BsonId) {
                classModelBuilder.idPropertyName(propertyModelBuilder.getName());
            } else if (annotation instanceof BsonIgnore) {
                propertyModelBuilder.readName(null);
            }
        }

        for (Annotation annotation : propertyModelBuilder.getWriteAnnotations()) {
            if (annotation instanceof BsonProperty) {
                BsonProperty bsonProperty = (BsonProperty) annotation;
                if (!"".equals(bsonProperty.value())) {
                    propertyModelBuilder.writeName(bsonProperty.value());
                }
            } else if (annotation instanceof BsonIgnore) {
                propertyModelBuilder.writeName(null);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void processCreatorAnnotation(final ClassModelBuilder<T> classModelBuilder) {
        Class<T> clazz = classModelBuilder.getType();
        CreatorExecutable<T> creatorExecutable = null;
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (isPublic(constructor.getModifiers())) {
                for (Annotation annotation : constructor.getDeclaredAnnotations()) {
                    if (annotation.annotationType().equals(BsonCreator.class)) {
                        creatorExecutable = new CreatorExecutable<T>(clazz, (Constructor<T>) constructor);
                        break;
                    }
                }
            }
        }

        Class<?> bsonCreatorClass = clazz;
        boolean foundStaticBsonCreatorMethod = false;
        while (bsonCreatorClass != null && !foundStaticBsonCreatorMethod) {
            for (Method method : bsonCreatorClass.getDeclaredMethods()) {
                if (isStatic(method.getModifiers())) {
                    for (Annotation annotation : method.getDeclaredAnnotations()) {
                        if (annotation.annotationType().equals(BsonCreator.class)) {
                            if (creatorExecutable != null) {
                                throw new CodecConfigurationException("Found multiple constructors / methods annotated with @BsonCreator");
                            } else if (!bsonCreatorClass.isAssignableFrom(method.getReturnType())) {
                                throw new CodecConfigurationException(
                                        format("Invalid method annotated with @BsonCreator. Returns '%s', expected %s",
                                                method.getReturnType(), bsonCreatorClass));
                            }
                            creatorExecutable = new CreatorExecutable<T>(clazz, method);
                            foundStaticBsonCreatorMethod = true;
                            break;
                        }
                    }
                }
            }

            bsonCreatorClass = bsonCreatorClass.getSuperclass();
        }

        if (creatorExecutable != null) {
            List<BsonProperty> properties = creatorExecutable.getProperties();
            List<Class<?>> parameterTypes = creatorExecutable.getParameterTypes();
            List<Type> parameterGenericTypes = creatorExecutable.getParameterGenericTypes();
            if (properties.size() != parameterTypes.size()) {
                throw creatorExecutable.getError(clazz, "All parameters must be annotated with a @BsonProperty in %s");
            }
            for (int i = 0; i < properties.size(); i++) {
                // BsonId may be missing, and thus creatorExecutable.getIdPropertyIndex() may be null.
                boolean isIdProperty = Integer.valueOf(i).equals(creatorExecutable.getIdPropertyIndex());
                Class<?> parameterType = parameterTypes.get(i);
                Type genericType = parameterGenericTypes.get(i);
                PropertyModelBuilder<?> propertyModelBuilder = null;

                if (isIdProperty) {
                    // This handles the BsonId annotation on the BsonCreator parameter.
                    propertyModelBuilder = classModelBuilder.getProperty(classModelBuilder.getIdPropertyName());
                } else {
                    BsonProperty bsonProperty = properties.get(i);

                    // Find the property using write name and falls back to read name
                    for (PropertyModelBuilder<?> builder : classModelBuilder.getPropertyModelBuilders()) {
                        if (bsonProperty.value().equals(builder.getWriteName())) {
                            // When there is a property that matches the write name of the parameter, use it and stop looking
                            propertyModelBuilder = builder;
                            break;
                        }

                        if (bsonProperty.value().equals(builder.getReadName())) {
                            // When there is a property that matches the read name of the parameter, save it but continue to look
                            // This is so just in case there is another property that matches the write name.
                            propertyModelBuilder = builder;
                        }
                    }

                    if (propertyModelBuilder == null) {
                        // BsonProperty value should be the BSON property name (write name). However, in legacy, when BsonProperty is used
                        // in BsonCreator parameters, it is mapped to the actual POJO property name (e.g. method name or field name).
                        // To support it, we still need to look it up.
                        propertyModelBuilder = classModelBuilder.getProperty(bsonProperty.value());
                    }

                    if (propertyModelBuilder == null) {
                        propertyModelBuilder = addCreatorPropertyToClassModelBuilder(classModelBuilder, bsonProperty.value(),
                            parameterType);
                    } else {
                        // An existing property is found, set its write name
                        propertyModelBuilder.writeName(bsonProperty.value());
                    }
                }

                if (parameterType.isAssignableFrom(propertyModelBuilder.getTypeData().getType())) {
                    // The existing getter for this field returns a more specific type than what the constructor accepts
                    // This is typical when the getter returns a specific subtype, but the constructor accepts a more
                    // general one (e.g.: getter returns ImmutableList<T>, while constructor just accepts List<T>)
                    ((PropertyModelBuilder<Object>)propertyModelBuilder).typeData(newTypeData(genericType, parameterType));
                } else if (!propertyModelBuilder.getTypeData().isAssignableFrom(parameterType)) {
                    throw creatorExecutable.getError(clazz, format("Invalid Property type for '%s'. Expected %s, found %s.",
                        propertyModelBuilder.getWriteName(), propertyModelBuilder.getTypeData().getType(), parameterType));
                }
            }
            classModelBuilder.instanceCreatorFactory(new InstanceCreatorFactoryImpl<T>(creatorExecutable));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> TypeData<T> newTypeData(final Type genericType, final Class<?> clazz) {
        return TypeData.newInstance(genericType, (Class<T>) clazz);
    }

    private <T, S> PropertyModelBuilder<S> addCreatorPropertyToClassModelBuilder(final ClassModelBuilder<T> classModelBuilder,
                                                                                 final String name,
                                                                                 final Class<S> clazz) {
        PropertyModelBuilder<S> propertyModelBuilder = createPropertyModelBuilder(new PropertyMetadata<S>(name,
            classModelBuilder.getType().getSimpleName(), TypeData.builder(clazz).build())).readName(null).writeName(name);
        classModelBuilder.addProperty(propertyModelBuilder);
        return propertyModelBuilder;
    }

    private void cleanPropertyBuilders(final ClassModelBuilder<?> classModelBuilder) {
        List<String> propertiesToRemove = new ArrayList<String>();
        for (PropertyModelBuilder<?> propertyModelBuilder : classModelBuilder.getPropertyModelBuilders()) {
            if (!propertyModelBuilder.isReadable() && !propertyModelBuilder.isWritable()) {
                propertiesToRemove.add(propertyModelBuilder.getName());
            }
        }
        for (String propertyName : propertiesToRemove) {
            classModelBuilder.removeProperty(propertyName);
        }
    }
}
