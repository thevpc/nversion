/**
 * ====================================================================
 * Nuts : Network Updatable Things Service
 * (universal package manager)
 * <br>
 * is a new Open Source Package Manager to help install packages
 * and libraries for runtime execution. Nuts is the ultimate companion for
 * maven (and other build managers) as it helps installing all package
 * dependencies at runtime. Nuts is not tied to java and is a good choice
 * to share shell scripts and other 'things' . Its based on an extensible
 * architecture to help supporting a large range of sub managers / repositories.
 * <br>
 * <p>
 * Copyright [2020] [thevpc]
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3 (the "License"); 
 * you may  not use this file except in compliance with the License. You may obtain
 * a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an 
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 * <br>
 * ====================================================================
 */
package net.thevpc.nuts.toolbox.nversion;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import net.thevpc.nuts.artifact.*;
import net.thevpc.nuts.core.NConstants;
import net.thevpc.nuts.core.NSession;
import net.thevpc.nuts.elem.NElementWriter;
import net.thevpc.nuts.io.NIOException;
import net.thevpc.nuts.io.NUncompressVisitor;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.io.NUncompress;
import net.thevpc.nuts.util.NBlankable;

/**
 * @author thevpc
 */
public class JarPathVersionResolver implements PathVersionResolver {
    public Set<VersionDescriptor> resolve(String filePath, NSession session) {
        if (filePath.endsWith(".jar") || filePath.endsWith(".war") || filePath.endsWith(".ear")) {

        } else {
            return null;
        }
        Set<VersionDescriptor> all = new HashSet<>();
        try (InputStream is = (NPath.of(filePath).toAbsolute()).inputStream()) {
            NUncompress.of()
                    .from(is)
                    .visit(new NUncompressVisitor() {
                        @Override
                        public boolean visitFolder(String path) {
                            return true;
                        }

                        @Override
                        public boolean visitFile(String path, InputStream inputStream) {
                            if ("META-INF/MANIFEST.MF".equals(path)) {
                                Manifest manifest = null;
                                try {
                                    manifest = new Manifest(inputStream);
                                } catch (IOException e) {
                                    throw new NIOException(e);
                                }
                                Attributes attrs = manifest.getMainAttributes();
                                String Bundle_SymbolicName = null;
                                String Bundle_Name = null;
                                String Bundle_Version = null;
                                Properties properties = new Properties();
                                for (Object o : attrs.keySet()) {
                                    Attributes.Name attrName = (Attributes.Name) o;
                                    String key = attrName.toString();
                                    String value = attrs.getValue(attrName);
                                    properties.setProperty(key, value);
                                    if ("Bundle-Version".equals(key)) {
                                        Bundle_Version = value;
                                    }
                                    if ("Bundle-SymbolicName".equals(key)) {
                                        Bundle_SymbolicName = value;
                                    }
                                    if ("Bundle-Name".equals(key)) {
                                        Bundle_Name = value;
                                    }
                                }
                                properties.setProperty("nuts.version-provider", "OSGI");
                                //OSGI
                                if (!NBlankable.isBlank(Bundle_SymbolicName)
                                        && !NBlankable.isBlank(Bundle_Name)
                                        && !NBlankable.isBlank(Bundle_Version)) {
                                    all.add(new VersionDescriptor(
                                            NIdBuilder.of().groupId(Bundle_SymbolicName).artifactId(Bundle_Name).version(Bundle_Version).build(),
                                            properties
                                    ));
                                }

                            } else if (("META-INF/" + NConstants.Files.DESCRIPTOR_FILE_NAME).equals(path)) {
                                try {
                                    NDescriptor d = NDescriptorParser.of().parse(inputStream).get();
                                    inputStream.close();
                                    Properties properties = new Properties();
                                    properties.setProperty("parents", d.parents().stream().map(Object::toString).collect(Collectors.joining(",")));
                                    properties.setProperty("name", d.id().artifactId());
                                    properties.setProperty("face", d.id().face());
                                    properties.setProperty("group", d.id().groupId());
                                    properties.setProperty("version", d.id().version().toString());
//                            if (d.getExt() != null) {
//                                properties.setProperty("ext", d.getExt());
//                            }
                                    if (d.packaging() != null) {
                                        properties.setProperty("packaging", d.packaging());
                                    }
                                    if (d.description() != null) {
                                        properties.setProperty("description", d.description());
                                    }
                                    properties.setProperty("locations", NElementWriter.ofJson().formatPlain(d.locations()));
                                    properties.setProperty(NConstants.IdProperties.ARCH, String.join(";", d.condition().arch()));
                                    properties.setProperty(NConstants.IdProperties.OS, String.join(";", d.condition().os()));
                                    properties.setProperty(NConstants.IdProperties.OS_DIST, String.join(";", d.condition().osDist()));
                                    properties.setProperty(NConstants.IdProperties.PLATFORM, String.join(";", d.condition().platform()));
                                    properties.setProperty(NConstants.IdProperties.DESKTOP, String.join(";", d.condition().desktopEnvironment()));
                                    properties.setProperty(NConstants.IdProperties.PROFILE, String.join(";", d.condition().profiles()));
                                    properties.setProperty("nuts.version-provider", NConstants.Files.DESCRIPTOR_FILE_NAME);
                                    if (d.properties() != null) {
                                        for (NDescriptorProperty e : d.properties()) {
                                            properties.put("property." + e.name(), e.value());
                                        }
                                    }
                                    all.add(new VersionDescriptor(d.id(), properties));
                                } catch (Exception e) {
                                    //e.printStackTrace();
                                }

                            } else if (path.startsWith("META-INF/maven/") && path.endsWith("/pom.xml")) {

                                Properties properties = new Properties();
                                try {
                                    NDescriptor d = NDescriptorParser.of()
                                            .descriptorStyle(NDescriptorStyle.MAVEN)
                                            .parse(inputStream).get();
                                    properties.put("groupId", d.id().groupId());
                                    properties.put("artifactId", d.id().artifactId());
                                    properties.put("version", d.id().version().toString());
                                    properties.put("name", d.name());
                                    properties.setProperty("nuts.version-provider", "maven");
                                    if (d.properties() != null) {
                                        for (NDescriptorProperty e : d.properties()) {
                                            properties.put("property." + e.name(), e.value());
                                        }
                                    }
                                    all.add(new VersionDescriptor(
                                            NIdBuilder.of().groupId(d.id().groupId())
                                                    .repository(d.id().artifactId())
                                                    .version(d.id().version())
                                                    .build(),
                                            properties));
                                } catch (Exception e) {
                                    //e.printStackTrace();
                                }

                            } else if (path.startsWith("META-INF/maven/") && path.endsWith("/pom.properties")) {
                                try {
                                    Properties prop = new Properties();
                                    try {
                                        prop.load(inputStream);
                                    } catch (IOException e) {
                                        //
                                    }
                                    String version = prop.getProperty("version");
                                    String groupId = prop.getProperty("groupId");
                                    String artifactId = prop.getProperty("artifactId");
                                    prop.setProperty("nuts.version-provider", "maven");
                                    if (version != null && version.trim().length() != 0) {
                                        all.add(new VersionDescriptor(
                                                NIdBuilder.of(groupId, artifactId).version(version)
                                                        .build(),
                                                prop
                                        ));
                                    }
                                } catch (Exception e) {
                                    //e.printStackTrace();
                                }

                            }
                            return true;
                        }
                    }).run();
        } catch (IOException ex) {
            throw new NIOException(ex);
        }
        return all;
    }
}
