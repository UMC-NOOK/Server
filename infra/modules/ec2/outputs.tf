output "instance_id" {
  value = aws_instance.this.id
}

output "private_ip" {
  value = aws_instance.this.private_ip
}

output "public_ip" {
  value = var.create_eip ? aws_eip.this[0].public_ip : aws_instance.this.public_ip
}

output "security_group_id" {
  value = aws_security_group.this.id
}
