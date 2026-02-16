[agendajuridicoappdev@agendajuridicoap
[agendajuridicoappdev@agendajuridicoappdev ~]$ uname -a
Linux agendajuridicoappdev 5.14.0-162.6.1.el9_1.x86_64 #1 SMP PREEMPT_DYNAMIC Fri Nov 18 02:06:38 UTC 2022 x86_64 x86_64 x86_64 GNU/Linux
[agendajuridicoappdev@agendajuridicoappdev ~]$ cat /etc/os-release 
NAME="Rocky Linux"
VERSION="9.7 (Blue Onyx)"
ID="rocky"
ID_LIKE="rhel centos fedora"
VERSION_ID="9.7"
PLATFORM_ID="platform:el9"
PRETTY_NAME="Rocky Linux 9.7 (Blue Onyx)"
ANSI_COLOR="0;32"
LOGO="fedora-logo-icon"
CPE_NAME="cpe:/o:rocky:rocky:9::baseos"
HOME_URL="https://rockylinux.org/"
VENDOR_NAME="RESF"
VENDOR_URL="https://resf.org/"
BUG_REPORT_URL="https://bugs.rockylinux.org/"
SUPPORT_END="2032-05-31"
ROCKY_SUPPORT_PRODUCT="Rocky-Linux-9"
ROCKY_SUPPORT_PRODUCT_VERSION="9.7"
REDHAT_SUPPORT_PRODUCT="Rocky Linux"
REDHAT_SUPPORT_PRODUCT_VERSION="9.7"
[agendajuridicoappdev@agendajuridicoappdev ~]$ lscpu
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
    Indicadores:                         fpu vme de pse tsc msr pae mce cx8 apic sep mtrr pge mca cmov pat pse36 clflush mmx fxsr sse sse2 ss ht syscall nx pdpe1gb rdtscp lm
                                          constant_tsc arch_perfmon rep_good nopl xtopology cpuid tsc_known_freq pni pclmulqdq vmx ssse3 fma cx16 pdcm pcid sse4_1 sse4_2 x2a
                                         pic movbe popcnt tsc_deadline_timer aes xsave avx f16c rdrand hypervisor lahf_lm abm 3dnowprefetch cpuid_fault invpcid_single ssbd i
                                         brs ibpb stibp ibrs_enhanced tpr_shadow vnmi flexpriority ept vpid ept_ad fsgsbase tsc_adjust bmi1 avx2 smep bmi2 erms invpcid mpx a
                                         vx512f avx512dq rdseed adx smap clflushopt clwb avx512cd avx512bw avx512vl xsaveopt xsavec xgetbv1 xsaves arat umip pku ospke avx512
                                         _vnni md_clear arch_capabilities
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
[agendajuridicoappdev@agendajuridicoappdev ~]$ free -h
               total        used        free      shared  buff/cache   available
Mem:            15Gi       1.4Gi        10Gi        90Mi       4.3Gi        13Gi
Swap:          7.9Gi          0B       7.9Gi
[agendajuridicoappdev@agendajuridicoappdev ~]$ lsbld
bash: lsbld: orden no encontrada
[agendajuridicoappdev@agendajuridicoappdev ~]$ lsblk
NAME        MAJ:MIN RM   SIZE RO TYPE MOUNTPOINTS
sda           8:0    0   300G  0 disk 
├─sda1        8:1    0     1G  0 part /boot
└─sda2        8:2    0   299G  0 part 
  ├─rl-root 253:0    0    70G  0 lvm  /
  ├─rl-swap 253:1    0   7.9G  0 lvm  [SWAP]
  └─rl-home 253:2    0 221.1G  0 lvm  /home
sr0          11:0    1  1024M  0 rom  
[agendajuridicoappdev@agendajuridicoappdev ~]$ df -h
S.ficheros          Tamaño Usados  Disp Uso% Montado en
devtmpfs              4.0M      0  4.0M   0% /dev
tmpfs                 7.7G      0  7.7G   0% /dev/shm
tmpfs                 3.1G    91M  3.0G   3% /run
/dev/mapper/rl-root    70G   3.9G   67G   6% /
/dev/mapper/rl-home   222G   2.3G  219G   2% /home
/dev/sda1            1014M   311M  703M  31% /boot
tmpfs                 1.6G   4.0K  1.6G   1% /run/user/1001
[agendajuridicoappdev@agendajuridicoappdev ~]$ hostnamectl
 Static hostname: agendajuridicoappdev
       Icon name: computer-vm
         Chassis: vm 🖴
      Machine ID: b68cc8d031584967bdc2aa7d702085b5
         Boot ID: 31d86bdd5b164cb096791541a6b451bf
  Virtualization: kvm
Operating System: Rocky Linux 9.7 (Blue Onyx)                 
     CPE OS Name: cpe:/o:rocky:rocky:9::baseos
          Kernel: Linux 5.14.0-162.6.1.el9_1.x86_64
    Architecture: x86-64
Firmware Version: rel-1.16.0-0-gd239552ce722-prebuilt.qemu.org
[agendajuridicoappdev@agendajuridicoappdev ~]$ 



Netid            State             Recv-Q            Send-Q                       Local Address:Port                        Peer Address:Port           Process                                                 
udp              UNCONN            0                 0                                127.0.0.1:323                              0.0.0.0:*               users:(("chronyd",pid=159606,fd=5))                    
udp              UNCONN            0                 0                                    [::1]:323                                 [::]:*               users:(("chronyd",pid=159606,fd=6))                    
tcp              LISTEN            0                 128                                0.0.0.0:22                               0.0.0.0:*               users:(("sshd",pid=140128,fd=3))                       
tcp              LISTEN            0                 1024                             127.0.0.1:33223                            0.0.0.0:*               users:(("code-bdd88df003",pid=224474,fd=11))           
tcp              LISTEN            0                 128                                   [::]:22                                  [::]:*               users:(("sshd",pid=140128,fd=4