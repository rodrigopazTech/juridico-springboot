import os
import re

directory = 'src/main/java'

for root, dirs, files in os.walk(directory):
    for file in files:
        if file.endswith('.java'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r') as f:
                content = f.read()
            
            # Replace RolUsuario.DIRECCION -> RolUsuario.DIRECTOR
            content = content.replace('RolUsuario.DIRECCION', 'RolUsuario.DIRECTOR')
            
            # Replace RolUsuario.SUBDIRECCION -> RolUsuario.SUBDIRECTOR
            content = content.replace('RolUsuario.SUBDIRECCION', 'RolUsuario.SUBDIRECTOR')

            # Replace findByGerenciaIdAndRolInAndActivoTrue -> findByGerenciasIdAndRolInAndActivoTrue
            content = content.replace('findByGerenciaIdAndRolInAndActivoTrue', 'findByGerenciasIdAndRolInAndActivoTrue')

            # Replace findByGerenciaIdAndActivoTrueOrderByNombreCompletoAsc -> findByGerenciasIdAndActivoTrueOrderByNombreCompletoAsc
            content = content.replace('findByGerenciaIdAndActivoTrueOrderByNombreCompletoAsc', 'findByGerenciasIdAndActivoTrueOrderByNombreCompletoAsc')

            # Replace findByRolAndGerenciaAndActivoTrue -> findByRolAndGerenciasIdAndActivoTrue
            content = re.sub(r'findByRolAndGerenciaAndActivoTrue\(([^,]+),\s*([^)]+)\)', r'findByRolAndGerenciasIdAndActivoTrue(\1, \2.getId())', content)
            
            # Since some places pass gerencia directly, but we need ID for the new method. Let's just do a naive replace and fix compilations.
            
            with open(filepath, 'w') as f:
                f.write(content)
print("Refactoring complete.")
