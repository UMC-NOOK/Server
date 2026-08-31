locals {
  name = "${var.project_name}-prod"
  common_tags = {
    Project     = var.project_name
    Environment = "prod"
    ManagedBy   = "Terraform"
  }
}

module "server" {
  source = "../../modules/ec2"

  name             = local.name
  vpc_id           = var.vpc_id
  subnet_id        = var.public_subnet_id
  ami_id           = var.ami_id
  instance_type    = var.instance_type
  key_name         = var.key_name
  create_eip       = var.create_eip
  root_volume_size = var.root_volume_size
  user_data        = file("${path.module}/../../modules/ec2/user-data.sh")

  ingress_rules = merge(
    {
      http = {
        description = "HTTP"
        from_port   = 80
        to_port     = 80
        protocol    = "tcp"
        cidr_blocks = ["0.0.0.0/0"]
      }
      https = {
        description = "HTTPS"
        from_port   = 443
        to_port     = 443
        protocol    = "tcp"
        cidr_blocks = ["0.0.0.0/0"]
      }
    },
    length(var.monitoring_cidrs) == 0 ? {} : {
      metrics = {
        description = "Spring metrics from monitoring server"
        from_port   = 9091
        to_port     = 9091
        protocol    = "tcp"
        cidr_blocks = var.monitoring_cidrs
      }
    },
    length(var.admin_cidrs) == 0 ? {} : {
      ssh = {
        description = "SSH from administrators"
        from_port   = 22
        to_port     = 22
        protocol    = "tcp"
        cidr_blocks = var.admin_cidrs
      }
    }
  )

  tags = local.common_tags
}
