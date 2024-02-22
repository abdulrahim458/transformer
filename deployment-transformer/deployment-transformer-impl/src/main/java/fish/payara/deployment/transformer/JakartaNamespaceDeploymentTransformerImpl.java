/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020-2023 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.deployment.transformer;

import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.util.LocalStringManagerImpl;
import fish.payara.deployment.transformer.api.JakartaNamespaceDeploymentTransformer;
import org.eclipse.transformer.payara.JakartaNamespaceTransformer;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.deployment.common.DeploymentException;
import org.glassfish.hk2.classmodel.reflect.Types;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.eclipse.transformer.Transformer.SUCCESS_RC;

/**
 * Transforms jakarta.* namespace to javax.* and vice-versa.
 *
 * @author gaurav.gupta@payara.fish
 */
public class JakartaNamespaceDeploymentTransformerImpl implements JakartaNamespaceDeploymentTransformer {

    private static final LocalStringManagerImpl localStrings = new LocalStringManagerImpl(
            JakartaNamespaceDeploymentTransformerImpl.class);

    private static final String[] COMMON_JAKARTA_CLASSES = {
            "jakarta.inject.Inject",
            "jakarta.servlet.http.HttpServlet",
            "jakarta.ws.rs.core.Application",
            "jakarta.persistence.Entity"
    };

	public File transformApplication(ExtendedDeploymentContext extendedDeploymentContext) throws IOException, DeploymentException {
		JakartaNamespaceTransformer transformer = new JakartaNamespaceTransformer(extendedDeploymentContext.getLogger(), extendedDeploymentContext.getSourceDir(), false);
		int result = transformer.run();
		if (result == SUCCESS_RC) {
			File output = transformer.getOutput();
			Path newPath = output.toPath();
			if (!Boolean.getBoolean(extendedDeploymentContext.getAppProps().getProperty(ServerTags.DIRECTORY_DEPLOYED))) {
				Files.walk(extendedDeploymentContext.getSourceDir().toPath())
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
				newPath = Files.move(output.toPath(), extendedDeploymentContext.getSourceDir().toPath());
				if (newPath == null) {
					String msg = localStrings.getLocalString("application.namespace.transform.failed", "Application namespace transformation failed");
					throw new DeploymentException(msg);
				}
			}
			return newPath.toFile();
		} else {
			String msg = localStrings.getLocalString("application.namespace.transform.failed", "Application namespace transformation failed");
			extendedDeploymentContext.getActionReport().failure(extendedDeploymentContext.getLogger(), msg);
			return null;
		}
	}

    @Override
    public boolean isJakartaEEApplication(Types types) {
        // Quick check for the most common Jakarta APIs
        for (String commonJakartaClass : COMMON_JAKARTA_CLASSES) {
            if (types.getBy(commonJakartaClass) != null) {
                return true;
            }
        }
        return false;
    }
}
