/**
 * ====================================================================
 *            Nuts : Network Updatable Things Service
 *                  (universal package manager)
 * <br>
 * is a new Open Source Package Manager to help install packages
 * and libraries for runtime execution. Nuts is the ultimate companion for
 * maven (and other build managers) as it helps installing all package
 * dependencies at runtime. Nuts is not tied to java and is a good choice
 * to share shell scripts and other 'things' . Its based on an extensible
 * architecture to help supporting a large range of sub managers / repositories.
 * <br>
 *
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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import net.thevpc.nuts.artifact.*;
import net.thevpc.nuts.core.NSession;

/**
 *
 * @author thevpc
 */
public class MavenFolderPathVersionResolver implements PathVersionResolver {

    public Set<VersionDescriptor> resolve(String filePath, NSession session) {
        if (Files.isRegularFile(Paths.get(filePath).resolve("pom.xml"))) {
            Properties properties = new Properties();
            Set<VersionDescriptor> all = new HashSet<>();
            try (InputStream inputStream = Files.newInputStream(Paths.get(filePath).resolve("pom.xml"))) {
                NDescriptor d = NDescriptorParser.of()
                        .descriptorStyle(NDescriptorStyle.MAVEN)
                        .parse(inputStream).get();

                putNonNull(properties,"groupId", d.id().groupId());
                putNonNull(properties,"artifactId", d.id().artifactId());
                putNonNull(properties,"version", d.id().version());
                putNonNull(properties,"name", d.name());
                properties.setProperty("nuts.version-provider", "maven");
                if (d.properties() != null) {
                    for (NDescriptorProperty e : d.properties()) {
                        putNonNull(properties,"property." + e.name(), e.value());
                    }
                }
                all.add(new VersionDescriptor(
                                NIdBuilder.of(d.id().groupId(),d.id().artifactId())
                                .version(d.id().version())
                                .build(),
                        properties));
            } catch (Exception e) {
//                e.printStackTrace();
            }
            return all;
        } else {
            return null;
        }
    }
    private void putNonNull(Properties p,String s,Object v){
        if(s==null || v==null){
            return;
        }
        p.put(s,v.toString());
    }
}
