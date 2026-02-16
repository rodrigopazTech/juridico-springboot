[agendajuridicodbdev@agendajuridicodbdev ~]$ uname -a
Linux agendajuridicodbdev 5.14.0-162.6.1.el9_1.x86_64 #1 SMP PREEMPT_DYNAMIC Fri Nov 18 02:06:38 UTC 2022 x86_64 x86_64 x86_64 GNU/Linux
[agendajuridicodbdev@agendajuridicodbdev ~]$ cat /etc/os-release 
NAME="Rocky Linux"
VERSION="9.1 (Blue Onyx)"
ID="rocky"
ID_LIKE="rhel centos fedora"
VERSION_ID="9.1"
PLATFORM_ID="platform:el9"
PRETTY_NAME="Rocky Linux 9.1 (Blue Onyx)"
ANSI_COLOR="0;32"
LOGO="fedora-logo-icon"
CPE_NAME="cpe:/o:rocky:rocky:9::baseos"
HOME_URL="https://rockylinux.org/"
BUG_REPORT_URL="https://bugs.rockylinux.org/"
ROCKY_SUPPORT_PRODUCT="Rocky-Linux-9"
ROCKY_SUPPORT_PRODUCT_VERSION="9.1"
REDHAT_SUPPORT_PRODUCT="Rocky Linux"
REDHAT_SUPPORT_PRODUCT_VERSION="9.1"
[agendajuridicodbdev@agendajuridicodbdev ~]$ lscpu
Arquitectura:                            x86_64
  modo(s) de operación de las CPUs:      32-bit, 64-bit
  Tamaños de las direcciones:            46 bits physical, 48 bits virtual
  Orden de los bytes:                    Little Endian
CPU(s):                                  8
  Lista de la(s) CPU(s) en línea:        0-7
ID de fabricante:                        GenuineIntel
  Nombre del modelo:                     Intel(R) Xeon(R) Gold 6248 CPU @ 2.50GHz
    Familia de CPU:                      6
    Modelo:                              85
    Hilo(s) de procesamiento por núcleo: 1
    Núcleo(s) por «socket»:              4
    «Socket(s)»:                         2
    Revisión:                            7
    BogoMIPS:                            5000.01
    Indicadores:                         fpu vme de pse tsc msr pae mce cx8 apic sep mtrr pge mca cmov pat pse36 clflush mmx fxsr sse sse2 ss ht syscall nx pdpe1gb rdtscp lm co
                                         nstant_tsc arch_perfmon rep_good nopl xtopology cpuid tsc_known_freq pni pclmulqdq vmx ssse3 fma cx16 pdcm pcid sse4_1 sse4_2 x2apic mo
                                         vbe popcnt tsc_deadline_timer aes xsave avx f16c rdrand hypervisor lahf_lm abm 3dnowprefetch cpuid_fault invpcid_single ssbd ibrs ibpb 
                                         stibp ibrs_enhanced tpr_shadow vnmi flexpriority ept vpid ept_ad fsgsbase tsc_adjust bmi1 avx2 smep bmi2 erms invpcid mpx avx512f avx51
                                         2dq rdseed adx smap clflushopt clwb avx512cd avx512bw avx512vl xsaveopt xsavec xgetbv1 xsaves arat umip pku ospke avx512_vnni md_clear 
                                         arch_capabilities
Características de virtualización:       
  Virtualización:                        VT-x
  Fabricante del hipervisor:             KVM
  Tipo de virtualización:                lleno
Cachés (suma de todas):                  
  L1d:                                   256 KiB (8 instancias)
  L1i:                                   256 KiB (8 instancias)
  L2:                                    32 MiB (8 instancias)
  L3:                                    32 MiB (2 instancias)
NUMA:                                    
  Modo(s) NUMA:                          1
  CPU(s) del nodo NUMA 0:                0-7
Vulnerabilidades:                        
  Itlb multihit:                         Not affected
  L1tf:                                  Not affected
  Mds:                                   Not affected
  Meltdown:                              Not affected
  Mmio stale data:                       Vulnerable: Clear CPU buffers attempted, no microcode; SMT Host state unknown
  Retbleed:                              Mitigation; Enhanced IBRS
  Spec store bypass:                     Mitigation; Speculative Store Bypass disabled via prctl
  Spectre v1:                            Mitigation; usercopy/swapgs barriers and __user pointer sanitization
  Spectre v2:                            Mitigation; Enhanced IBRS, IBPB conditional, RSB filling, PBRSB-eIBRS SW sequence
  Srbds:                                 Not affected
  Tsx async abort:                       Mitigation; TSX disabled
[agendajuridicodbdev@agendajuridicodbdev ~]$ free -h
               total        used        free      shared  buff/cache   available
Mem:            15Gi       1.9Gi       9.1Gi        59Mi       4.7Gi        13Gi
Swap:          7.9Gi          0B       7.9Gi
[agendajuridicodbdev@agendajuridicodbdev ~]$ lsblk
NAME        MAJ:MIN RM   SIZE RO TYPE MOUNTPOINTS
sda           8:0    0   300G  0 disk 
├─sda1        8:1    0     1G  0 part /boot
└─sda2        8:2    0   299G  0 part 
  ├─rl-root 253:0    0    70G  0 lvm  /
  ├─rl-swap 253:1    0   7.9G  0 lvm  [SWAP]
  └─rl-home 253:2    0 221.1G  0 lvm  /home
sr0          11:0    1  1024M  0 rom  
[agendajuridicodbdev@agendajuridicodbdev ~]$ df -h
S.ficheros          Tamaño Usados  Disp Uso% Montado en
devtmpfs              4.0M      0  4.0M   0% /dev
tmpfs                 7.7G    84K  7.7G   1% /dev/shm
tmpfs                 3.1G    42M  3.1G   2% /run
/dev/mapper/rl-root    70G   4.1G   66G   6% /
/dev/mapper/rl-home   222G   4.0G  218G   2% /home
/dev/sda1            1014M   244M  771M  24% /boot
tmpfs                 1.6G      0  1.6G   0% /run/user/1002
tmpfs                 1.6G      0  1.6G   0% /run/user/1001
[agendajuridicodbdev@agendajuridicodbdev ~]$ sydo lshw -short
bash: sydo: orden no encontrada
[agendajuridicodbdev@agendajuridicodbdev ~]$ sudo lshw -short
[sudo] password for agendajuridicodbdev: 
H/W path              Device      Class      Description
========================================================
                                  system     Standard PC (i440FX + PIIX, 1996)
/0                                bus        Motherboard
/0/0                              memory     96KiB BIOS
/0/400                            processor  Intel(R) Xeon(R) Gold 6248 CPU @ 2.50GHz
/0/401                            processor  Intel(R) Xeon(R) Gold 6248 CPU @ 2.50GHz
/0/1000                           memory     16GiB System Memory
/0/1000/0                         memory     16GiB DIMM RAM
/0/100                            bridge     440FX - 82441FX PMC [Natoma]
/0/100/1                          bridge     82371SB PIIX3 ISA [Natoma/Triton II]
/0/100/1/0                        input      PnP device PNP0303
/0/100/1/1                        input      PnP device PNP0f13
/0/100/1/2                        storage    PnP device PNP0700
/0/100/1/3                        system     PnP device PNP0b00
/0/100/1.1            scsi2       storage    82371SB PIIX3 IDE [Natoma/Triton II]
/0/100/1.1/0.0.0      /dev/cdrom  disk       QEMU DVD-ROM
/0/100/1.2                        bus        82371SB PIIX3 USB [Natoma/Triton II]
/0/100/1.2/1          usb1        bus        UHCI Host Controller
/0/100/1.2/1/1        input5      input      QEMU QEMU USB Tablet
/0/100/1.3                        bridge     82371AB/EB/MB PIIX4 ACPI
/0/100/2              /dev/fb0    display    bochs-drmdrmfb
/0/100/3                          generic    Virtio memory balloon
/0/100/3/0                        generic    Virtual I/O device
/0/100/5                          bridge     QEMU PCI-PCI bridge
/0/100/5/1                        storage    Virtio SCSI
/0/100/5/1/0          scsi0       generic    Virtual I/O device
/0/100/5/1/0/0.0.0    /dev/sda    disk       322GB QEMU HARDDISK
/0/100/5/1/0/0.0.0/1              volume     1GiB Linux filesystem partition
/0/100/5/1/0/0.0.0/2  /dev/sda2   volume     298GiB Linux LVM Physical Volume partition
/0/100/12                         network    Virtio network device
/0/100/12/0           ens18       network    Ethernet interface
/0/100/13                         network    Virtio network device
/0/100/13/0           ens19       network    Ethernet interface
/0/100/1e                         bridge     QEMU PCI-PCI bridge
/0/100/1f                         bridge     QEMU PCI-PCI bridge
/1                    input0      input      Power Button
/2                    input1      input      AT Translated Set 2 keyboard
/3                    input3      input      VirtualPS/2 VMware VMMouse
/4                    input4      input      VirtualPS/2 VMware VMMouse
/5                    input6      input      PC Speaker
[agendajuridicodbdev@agendajuridicodbdev ~]$ sudo dnf install lshw -y
Última comprobación de caducidad de metadatos hecha hace 2:28:54, el mié 11 feb 2026 10:00:11.
El paquete lshw-B.02.19.2-9.el9.x86_64 ya está instalado.
Dependencias resueltas.
================================================================================================================================================================================
 Paquete                                Arquitectura                             Versión                                         Repositorio                               Tam.
================================================================================================================================================================================
Actualizando:
 lshw                                   x86_64                                   B.02.20-2.el9                                   baseos                                   329 k

Resumen de la transacción
================================================================================================================================================================================
Actualizar  1 Paquete

Tamaño total de la descarga: 329 k
Descargando paquetes:
lshw-B.02.20-2.el9.x86_64.rpm                                                                                                                   476 kB/s | 329 kB     00:00    
--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
Total                                                                                                                                           328 kB/s | 329 kB     00:01     
Ejecutando verificación de operación
Verificación de operación exitosa.
Ejecutando prueba de operaciones
Prueba de operación exitosa.
Ejecutando operación
  Preparando          :                                                                                                                                                     1/1 
  Actualizando        : lshw-B.02.20-2.el9.x86_64                                                                                                                           1/2 
  Limpieza            : lshw-B.02.19.2-9.el9.x86_64                                                                                                                         2/2 
  Ejecutando scriptlet: lshw-B.02.19.2-9.el9.x86_64                                                                                                                         2/2 
  Verificando         : lshw-B.02.20-2.el9.x86_64                                                                                                                           1/2 
  Verificando         : lshw-B.02.19.2-9.el9.x86_64                                                                                                                         2/2 

Actualizado:
  lshw-B.02.20-2.el9.x86_64                                                                                                                                                     

¡Listo!
[agendajuridicodbdev@agendajuridicodbdev ~]$ hostnamectl
 Static hostname: agendajuridicodbdev
       Icon name: computer-vm
         Chassis: vm 🖴
      Machine ID: d5e862fa6d274663808469cfd625a530
         Boot ID: 5699fae110de4d7196dbdecf133313cd
  Virtualization: kvm
Operating System: Rocky Linux 9.1 (Blue Onyx)      
     CPE OS Name: cpe:/o:rocky:rocky:9::baseos
          Kernel: Linux 5.14.0-162.6.1.el9_1.x86_64
    Architecture: x86-64
 Hardware Vendor: QEMU
  Hardware Model: Standard PC _i440FX + PIIX, 1996_
[agendajuridicodbdev@agendajuridicodbdev ~]$ 

PUERTOS

Netid            State             Recv-Q            Send-Q                       Local Address:Port                        Peer Address:Port           Process                                                 
udp              UNCONN            0                 0                                127.0.0.1:323                              0.0.0.0:*               users:(("chronyd",pid=159606,fd=5))                    
udp              UNCONN            0                 0                                    [::1]:323                                 [::]:*               users:(("chronyd",pid=159606,fd=6))                    
tcp              LISTEN            0                 128                                0.0.0.0:22                               0.0.0.0:*               users:(("sshd",pid=140128,fd=3))                       
tcp              LISTEN            0                 1024                             127.0.0.1:33223                            0.0.0.0:*               users:(("code-bdd88df003",pid=224474,fd=11))           
tcp              LISTEN            0                 128                                   [::]:22                                  [::]:*               users:(("sshd",pid=140128,fd=4))        