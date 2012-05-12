/**********************************************************************
Copyright (c) 2008 Erik Bengtson and others. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Contributors:
2008 Andy Jefferson - clean up
    ...
 **********************************************************************/
package org.datanucleus.store.xml.binder;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.metadata.Relation;
import org.w3c.dom.Node;

import com.sun.xml.bind.api.JAXBRIContext;
import com.sun.xml.bind.v2.model.annotation.AbstractInlineAnnotationReaderImpl;
import com.sun.xml.bind.v2.model.annotation.Locatable;
import com.sun.xml.bind.v2.model.annotation.LocatableAnnotation;
import com.sun.xml.bind.v2.model.annotation.RuntimeAnnotationReader;
import com.sun.xml.bind.v2.model.annotation.RuntimeInlineAnnotationReader;

/**
 * Binds annotations to the JAXB context using the JAXB reference implementation.
 * It reads the JDO/JPA metadata and binds JAXB annotations to the JAXB context, allowing
 * marshalling/unmarshalling of objects from/to XML.
 * <p>
 * Class annotations
 * <ul>
 * <li>{@link javax.xml.bind.annotation.XmlRootElement}</li>
 * <li>{@link javax.xml.bind.annotation.XmlType}</li>
 * <li>{@link javax.xml.bind.annotation.XmlAccessorType}</li>
 * </ul>
 * </p>
 * <p>
 * Field annotations
 * <ul>
 * <li>{@link javax.xml.bind.annotation.XmlAttribute}</li>
 * <li>{@link javax.xml.bind.annotation.XmlElement}</li>
 * <li>{@link javax.xml.bind.annotation.XmlElementRef}</li>
 * <li>{@link javax.xml.bind.annotation.XmlElementWrapper}</li>
 * <li>{@link javax.xml.bind.annotation.XmlIDREF}</li>
 * <li>{@link javax.xml.bind.annotation.XmlID}</li>
 * </ul>
 * </p>
 * <p>
 * Method annotations. The current implementation marks all methods as transient.
 * <ul>
 * <li>{@link javax.xml.bind.annotation.XmlTransient}</li>
 * </ul>
 * </p>
 */
public class JAXBRuntimeBinder extends AbstractInlineAnnotationReaderImpl<Type, Class, Field, Method> implements RuntimeAnnotationReader
{
    private MetaDataManager metaDataMgr;
    private ClassLoaderResolver clr;
    private RuntimeAnnotationReader baseReader = new RuntimeInlineAnnotationReader();

    /**
     * Method to marshall an object into XML for storing.
     * @param obj The object
     * @param node The node where we store it
     * @param mmgr MetaData manager
     * @param clr ClassLoader resolver
     * @throws JAXBException
     */
    public static void marshall(Object obj, Node node, MetaDataManager mmgr, ClassLoaderResolver clr) 
    throws JAXBException
    {
        Map<String, Object> jaxbConfig = new HashMap<String, Object>();
        JAXBRuntimeBinder annReader = new JAXBRuntimeBinder(mmgr, clr);
        //jaxbConfig.put(JAXBRIContext.DEFAULT_NAMESPACE_REMAP, "DefaultNamespace");
        jaxbConfig.put(JAXBRIContext.ANNOTATION_READER, annReader);
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        ClassLoader cls = JAXBRIContext.class.getClassLoader();
        
        Thread.currentThread().setContextClassLoader(cls);
        JAXBContext jaxbContext = JAXBContext.newInstance(new Class[] {obj.getClass()}, jaxbConfig);
        Thread.currentThread().setContextClassLoader(cl);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(obj, node);
    }

    /**
     * Method to unmarshall a node from XML into an object.
     * @param cls Type of object
     * @param node The node to be unmarshalled
     * @param mmgr MetaData manager
     * @param clr ClassLoader resolver
     * @throws JAXBException
     */
    public static Object unmarshall(Class cls, Node node, MetaDataManager mmgr, ClassLoaderResolver clr) 
    throws JAXBException
    {
        Map<String, Object> jaxbConfig = new HashMap<String, Object>();
        JAXBRuntimeBinder annReader = new JAXBRuntimeBinder(mmgr, clr);
        //jaxbConfig.put(JAXBRIContext.DEFAULT_NAMESPACE_REMAP, "DefaultNamespace");
        jaxbConfig.put(JAXBRIContext.ANNOTATION_READER, annReader);

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        ClassLoader clazz = JAXBRIContext.class.getClassLoader();
        Thread.currentThread().setContextClassLoader(clazz);
        JAXBContext jaxbContext = JAXBContext.newInstance(new Class[] {cls}, jaxbConfig);
        Thread.currentThread().setContextClassLoader(cl);
        return jaxbContext.createUnmarshaller().unmarshal(node);
    }

    public JAXBRuntimeBinder(MetaDataManager metaDataMgr, ClassLoaderResolver clr)
    {
        this.metaDataMgr = metaDataMgr;
        this.clr = clr;
    }

    protected String fullName(Method method)
    {
        return method.getDeclaringClass().getName() + '#' + method.getName();
    }

    /**
     * Method to return all (JAXB) annotations on the specified field.
     * @param field The field
     * @param loc Location from which this is read
     * @return the annotations
     */
    public Annotation[] getAllFieldAnnotations(Field field, Locatable loc)
    {
        Annotation[] anns = ((AnnotatedElement) field).getAnnotations();
        List<Annotation> annotations = new ArrayList<Annotation>();

        for (int i = 0; i < anns.length; i++)
        {
            Class<? extends Object> type = anns[i].getClass();
            if (!isSupportedMemberAnnotation(type))
            {
                annotations.add(LocatableAnnotation.create(anns[i], loc));
            }
        }
        AbstractClassMetaData acmd = metaDataMgr.getMetaDataForClass(field.getDeclaringClass(), clr);
        AbstractMemberMetaData ammd = acmd.getMetaDataForMember(field.getName());
        if (ammd != null)
        {
            List<Annotation> fieldAnnots = generateAnnotationsForMember(ammd, field, loc);
            annotations.addAll(fieldAnnots);
        }

        return annotations.toArray(new Annotation[annotations.size()]);
    }

    /**
     * Method to return all (JAXB) annotations on the specified method.
     * @param method The method
     * @param loc Location from which this is read
     * @return the annotations
     */
    public Annotation[] getAllMethodAnnotations(Method method, Locatable loc)
    {
        Annotation[] anns = ((AnnotatedElement) method).getAnnotations();
        List<Annotation> annotations = new ArrayList<Annotation>();

        for (int i = 0; i < anns.length; i++)
        {
            Class<? extends Object> type = anns[i].getClass();
            if (!isSupportedMemberAnnotation(type))
            {
                annotations.add(LocatableAnnotation.create(anns[i], loc));
            }
        }
        /*AbstractClassMetaData acmd = metaDataMgr.getMetaDataForClass(method.getDeclaringClass(), clr);
        AbstractMemberMetaData ammd = acmd.getMetaDataForMember(method.getName());
        if (ammd != null && ammd.getPersistenceModifier() == FieldPersistenceModifier.PERSISTENT)
        {
            List<Annotation> methodAnnots = generateAnnotationsForMember(ammd, method, loc);
            annotations.addAll(methodAnnots);
        }*/
        return annotations.toArray(new Annotation[annotations.size()]);
    }

    public <A extends Annotation> A getClassAnnotation(Class<A> anntype, Class type, Locatable loc)
    {
        Annotation proxy = getAnnotationHandler(anntype, type);
        if (proxy != null)
        {
           return (A)proxy;
        }
        return LocatableAnnotation.create(((Class<?>)type).getAnnotation(anntype), loc);
    }
    
    public Type[] getClassArrayValue(Annotation a, String name)
    {
        return baseReader.getClassArrayValue(a, name);
    }

    public Type getClassValue(Annotation a, String name)
    {
        return baseReader.getClassValue(a, name);
    }

    public <A extends Annotation> A getFieldAnnotation(Class<A> type, Field field, Locatable loc)
    {
        Annotation proxy = getProxy(type, field);
        if (proxy != null)
        {
            return (A) proxy;
        }
        return LocatableAnnotation.create(field.getAnnotation(type), loc);
    }

    public <A extends Annotation> A getMethodAnnotation(Class<A> type, Method method, Locatable loc)
    {
        Annotation proxy = getProxy(type, method);
        if (proxy != null)
        {
            return (A) proxy;
        }
        return LocatableAnnotation.create(method.getAnnotation(type), loc);
    }

    public <A extends Annotation> A getMethodParameterAnnotation(Class<A> arg0, Method arg1, int arg2, Locatable arg3)
    {
        return null;
    }

    public <A extends Annotation> A getPackageAnnotation(Class<A> arg0, Class arg1, Locatable arg2)
    {
        return null;
    }

    public boolean hasClassAnnotation(Class type, Class<? extends Annotation> anntype)
    {
        if (anntype != XmlRootElement.class &&  anntype != XmlType.class && anntype != XmlAccessorType.class)
        {
            return false;
        }
        AbstractClassMetaData acmd = metaDataMgr.getMetaDataForClass(type, clr);
        return acmd != null;
    }

    public boolean hasFieldAnnotation(Class<? extends Annotation> anntype, Field field)
    {
        if (!isSupportedMemberAnnotation(anntype))
        {
            return false;
        }
        AbstractClassMetaData acmd = metaDataMgr.getMetaDataForClass(field.getDeclaringClass(), clr);
        AbstractMemberMetaData ammd = acmd.getMetaDataForMember(field.getName());
        if (ammd == null || ammd.getPersistenceModifier() != FieldPersistenceModifier.PERSISTENT)
        {
            return false;
        }

        if (anntype == javax.xml.bind.annotation.XmlID.class)
        {
            // Same logic as we have in generateAnnotationsForMember(...)
            if (ammd.getAbstractClassMetaData().getNoOfPrimaryKeyMembers() == 1 && ammd.isPrimaryKey())
            {
                return true;
            }
            return false;
        }
        if (anntype == javax.xml.bind.annotation.XmlIDREF.class)
        {
            // Same logic as we have in generateAnnotationsForMember(...)
            if (ammd.getEmbeddedMetaData() == null && ammd.getRelationType(clr) != Relation.NONE)
            {
                return true;
            }
            return false;
        }
        return true;
    }

    public boolean hasMethodAnnotation(Class<? extends Annotation> annotation, String propertyName, 
            Method getter, Method setter, Locatable srcPos)
    {
        // TODO Implement this
        return super.hasMethodAnnotation(annotation, propertyName, getter, setter, srcPos);
    }

    public boolean hasMethodAnnotation(Class<? extends Annotation> anntype, Method method)
    {
        if (!isSupportedMemberAnnotation(anntype))
        {
            return false;
        }
        AbstractClassMetaData acmd = metaDataMgr.getMetaDataForClass(method.getDeclaringClass(), clr);
        AbstractMemberMetaData ammd = acmd.getMetaDataForMember(method.getName());
        if (ammd == null || ammd.getPersistenceModifier() != FieldPersistenceModifier.PERSISTENT)
        {
            return false;
        }

        // TODO Implement this
        if (anntype == XmlTransient.class)
        {
            //return true;
        }
        return false;
    }

    /**
     * Method to return the annotations for the specified member (field/property).
     * @param ammd Metadata for the member
     * @param member The member
     * @param loc Locatable
     * @return The JAXB annotations required
     */
    private List<Annotation> generateAnnotationsForMember(AbstractMemberMetaData ammd, Member member,
        Locatable loc)
    {
        List<Annotation> annotations = new ArrayList<Annotation>();

        if (ammd.getPersistenceModifier() == FieldPersistenceModifier.NONE)
        {
            Annotation annotation = getAnnotationHandler(XmlTransient.class);
            if (annotation != null)
            {
                annotations.add(annotation);
            }
        }
        else if (ammd.hasExtension("XmlAttribute"))
        {
            Annotation annotation = getAnnotation(XmlAttribute.class, ammd, member, loc);
            if (annotation != null)
            {
                annotations.add(annotation);
            }
        }
        else if (ammd.hasExtension("XmlElement"))
        {
            Annotation annotation = getAnnotation(XmlElement.class, ammd, member, loc);
            if (annotation != null)
            {
                annotations.add(annotation);
            }
        }
        else if (ammd.hasExtension("XmlElementRef"))
        {
            Annotation annotation = getAnnotation(XmlElementRef.class, ammd, member, loc);
            if (annotation != null)
            {
                annotations.add(annotation);
            }
        }
        else if (ammd.hasExtension("XmlElementWrapper"))
        {
            Annotation annotation = getAnnotation(XmlElementWrapper.class, ammd, member, loc);
            if (annotation != null)
            {
                annotations.add(annotation);
            }
        }
        else if (ammd.hasExtension("XmlIDREF"))
        {
            Annotation annotation = getAnnotation(XmlIDREF.class, ammd, member, loc);
            if (annotation != null)
            {
                annotations.add(annotation);
            }
        }
        else if (ammd.hasExtension("XmlID"))
        {
            Annotation annotation = getAnnotation(XmlID.class, ammd, member, loc);
            if (annotation != null)
            {
                annotations.add(annotation);
            }
        }
        else if (ammd.isPrimaryKey())
        {
            // All PK fields should have XmlID
            // Note : JAXB has a limitation of 1 XmlID field per class!!
            if (ammd.getAbstractClassMetaData().getNoOfPrimaryKeyMembers() > 1)
            {
                // TODO Remove this when we remove the limit on PK field numbers
                throw new NucleusException("Class " + ammd.getAbstractClassMetaData().getFullClassName() + " has more than 1 primary key field - not valid for JAXB");
            }
            if (ammd.getAbstractClassMetaData().getNoOfPrimaryKeyMembers() == 1)
            {
                if (!String.class.isAssignableFrom(ammd.getType()))
                {
                    // TODO Remove this when we remove the limit on PK field types
                    throw new NucleusException("Class " + ammd.getAbstractClassMetaData().getFullClassName() +
                        " has primary-key field " + ammd.getName() + " but this is not a String type - not valid for JAXB");
                }
                Annotation annotation = getAnnotation(XmlID.class, ammd, member, loc);
                if (annotation != null)
                {
                    annotations.add(annotation);
                }
            }
        }
        else if (ammd.getEmbeddedMetaData() == null && ammd.getRelationType(clr) != Relation.NONE)
        {
            // Relation and not embedded so give it an XmlIDREF so we just refer to the other object(s)
            Annotation annotation = getAnnotation(XmlIDREF.class, ammd, member, loc);
            if (annotation != null)
            {
                annotations.add(annotation);
            }
        }
        return annotations;
    }

    private boolean isSupportedMemberAnnotation(Class anntype)
    {
        if (anntype == javax.xml.bind.annotation.XmlAttribute.class)
        {
            return true;
        }
        else if (anntype == javax.xml.bind.annotation.XmlElement.class)
        {
            return true;
        }
        else if (anntype == javax.xml.bind.annotation.XmlElementRef.class)
        {
            return true;
        }
        else if (anntype == javax.xml.bind.annotation.XmlElementWrapper.class)
        {
            return true;
        }
        else if (anntype == javax.xml.bind.annotation.XmlID.class)
        {
            return true;
        }
        else if (anntype == javax.xml.bind.annotation.XmlIDREF.class)
        {
            return true;
        }
        return false;
    }

    private Annotation getAnnotation(Class type, AbstractMemberMetaData ammd, Member member, Locatable loc)
    {
        Annotation annotation = getAnnotationHandler(type, ammd);
        if (annotation != null)
        {
            return annotation;
        }
        annotation = ((AnnotatedElement)member).getAnnotation(type);
        if (annotation != null)
        {
            return LocatableAnnotation.create(annotation, loc);
        }
        return null;
    }

    private Annotation getAnnotationHandler(Class anntype, AbstractMemberMetaData ammd)
    {
        if (anntype == javax.xml.bind.annotation.XmlAttribute.class)
        {
            return XmlAttributeHandler.newProxy(ammd);
        }
        else if (anntype == javax.xml.bind.annotation.XmlElement.class)
        {
            return XmlElementHandler.newProxy(ammd, clr);
        }
        else if (anntype == javax.xml.bind.annotation.XmlElementRef.class)
        {
            return XmlElementRefHandler.newProxy(ammd, clr);
        }
        else if (anntype == javax.xml.bind.annotation.XmlElementWrapper.class)
        {
            if (ammd.hasCollection() || ammd.hasMap() || ammd.hasArray())
            {
                return XmlElementWrapperHandler.newProxy(ammd);
            }
        }
        else if (anntype == javax.xml.bind.annotation.XmlID.class)
        {
            return XmlIDHandler.newProxy(ammd);
        }
        else if (anntype == javax.xml.bind.annotation.XmlIDREF.class)
        {
            return XmlIDREFHandler.newProxy(ammd);
        }

        return null;
    }

    private Annotation getAnnotationHandler(Class anntype)
    {
        if (anntype == javax.xml.bind.annotation.XmlTransient.class)
        {
            return XmlTransientHandler.newProxy();
        }

        return null;
    }

    private Annotation getAnnotationHandler(Class anntype, Class type)
    {
        AbstractClassMetaData acmd = metaDataMgr.getMetaDataForClass(type, clr);
        
        if (acmd==null)
        {
            //happens when type is enum
            return null;
        }
        if (anntype == javax.xml.bind.annotation.XmlType.class)
        {
            return XmlTypeHandler.newProxy(acmd);
        }
        else if (anntype == javax.xml.bind.annotation.XmlRootElement.class)
        {
            return XmlRootElementHandler.newProxy(acmd);
        }
        else if (anntype == javax.xml.bind.annotation.XmlAccessorType.class)
        {
            return XmlAccessorTypeHandler.newProxy(acmd);
        }
        return null;
    }

    private Annotation getProxy(Class type, Field field)
    {
        AbstractClassMetaData acmd = metaDataMgr.getMetaDataForClass(field.getDeclaringClass(), clr);
        if (acmd == null)
        {
            //happens when field type is enum
            return null;
        }        
        AbstractMemberMetaData ammd = acmd.getMetaDataForMember(field.getName());
        if (ammd == null)
        {
            return null;
        }
        return getAnnotationHandler(type, ammd);
    }
    
    private Annotation getProxy(Class type, Method method)
    {
        AbstractClassMetaData acmd = metaDataMgr.getMetaDataForClass(method.getDeclaringClass(), clr);
        AbstractMemberMetaData ammd = acmd.getMetaDataForMember(method.getName());
        if (ammd == null)
        {
            return null;
        }
        return getAnnotationHandler(type, ammd);
    }
}