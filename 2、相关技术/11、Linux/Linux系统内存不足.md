确保系统有足够的内存和交换空间。可以使用以下命令检查系统的内存状态：

```shell
free -m
```

如果内存不足，可以增加交换空间：

```shell
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
sudo sh -c 'echo "/swapfile swap swap defaults 0 0" >> /etc/fstab'
```