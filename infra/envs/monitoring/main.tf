locals {
  name = "${var.project_name}-monitoring"
  common_tags = {
    Project     = var.project_name
    Environment = "monitoring"
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
    length(var.admin_cidrs) == 0 ? {} : {
      grafana = {
        description = "Grafana from administrators"
        from_port   = 3000
        to_port     = 3000
        protocol    = "tcp"
        cidr_blocks = var.admin_cidrs
      }
      ssh = {
        description = "SSH from administrators"
        from_port   = 22
        to_port     = 22
        protocol    = "tcp"
        cidr_blocks = var.admin_cidrs
      }
    },
    length(var.application_cidrs) == 0 ? {} : {
      loki = {
        description = "Loki pushes from application servers"
        from_port   = 3100
        to_port     = 3100
        protocol    = "tcp"
        cidr_blocks = var.application_cidrs
      }
    }
  )

  tags = local.common_tags
}
