# run.py - 启动脚本
#!/usr/bin/env python3
import os
from app import create_app

if __name__ == '__main__':
    # 设置环境
    os.environ.setdefault('FLASK_CONFIG', 'development')

    app = create_app()

    # 启动应用
    app.run(
        host=app.config['HOST'],
        port=app.config['PORT'],
        debug=app.config['DEBUG']
    )