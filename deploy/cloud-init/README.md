# Initialize an Ubuntu VM with FnFlow Platform

## On Ubuntu
- Create a virtual machine with fnflow
    ```shell
    multipass launch -n fnflow --cpus 4 --disk 20G --memory 8G --cloud-init Ubuntu-24.04.user-data
    ```
- Open a shell into the VM
    ```shell
    multipass exec fnflow -- bash
    ```
- Initialize the platform (execute inside the vm)
    ```shell
    cd fnflow/deploy
    helmfile -e local apply
    ```
## On Windows using WSL2
### Install wsl and Ubuntu distro
- In Windows terminal as administrator run `wsl --install`
- Reboot
- Run the following `wsl --install -d Ubuntu-24.04`

### Initialize an instance with fnflow
- Create a directory `.cloud-init` inside your user directory
- Copy the file [Ubuntu-24.04.user-data](./Ubuntu-24.04.user-data) into the newly created directory
- Run `ubuntu2404.exe` in powershell and wait until the new instance is initialized
- Execute `cd fnflow/deploy && helmfile -e local apply` in the opened terminal
