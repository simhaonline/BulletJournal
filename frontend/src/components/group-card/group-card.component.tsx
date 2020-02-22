import React from 'react';
import { Menu, Icon, Button, List, Dropdown, Badge, Avatar } from 'antd';
import { connect } from 'react-redux';
import { deleteGroup } from '../../features/group/actions';
import { Group, User } from '../../features/group/interfaces';
import { MyselfWithAvatar } from '../../features/myself/reducer';
import { IState } from '../../store';

import './group-card.styles.less';

type GroupProps = {
  group: Group;
  myself: MyselfWithAvatar;
  deleteGroup: (groupId: number) => void;
};

function getGroupUserTitle(item: User, group: Group): string {
  if (item.name === group.owner) {
    return 'Owner';
  }
  return item.accepted ? 'Joined' : 'Not Joined';
}

function getGroupUserSpan(item: User, group: Group): JSX.Element {
  if (item.name === group.owner) {
    return (
      <span>
        &nbsp;&nbsp;<strong>{item.name}</strong>
      </span>
    );
  }
  if (item.accepted) {
    return <span>&nbsp;&nbsp;{item.name}</span>;
  }

  return <span style={{ color: 'grey' }}>&nbsp;&nbsp;{item.name}</span>;
}

class GroupCard extends React.Component<GroupProps> {
  handleDelete = (groupId: number) => {
    this.props.deleteGroup(groupId);
  };

  handleMenuClick = (menu: any, groupId: number) => {
    if (menu.key === 'delete') {
      this.handleDelete(groupId);
    }
  };
  render() {
    const { group } = this.props;
    return (
      <div className="group-card">
        <div className="group-title">
          <h3>{`Group "${group.name}"`}</h3>
          <h3 className="group-operation">
            <Icon type="user" />
            {group.users && group.users.length}
            {group.owner === this.props.myself.username && (
              <Dropdown
                overlay={
                  <Menu onClick={menu => this.handleMenuClick(menu, group.id)}>
                    <Menu.Item key="edit">
                      <Icon type="edit" /> Edit
                    </Menu.Item>
                    <Menu.Divider />
                    <Menu.Item
                      key="delete"
                      disabled={this.props.group.owner === ''}
                    >
                      <Icon type="delete" /> Delete
                    </Menu.Item>
                  </Menu>
                }
                trigger={['click']}
                placement="bottomLeft"
              >
                <Button type="link" className="group-setting">
                  <Icon type="setting" title="Edit Group" />
                </Button>
              </Dropdown>
            )}
          </h3>
        </div>
        <div className="group-users">
          <List
            dataSource={group.users}
            renderItem={item => {
              return (
                <List.Item key={item.id}>
                  <div
                    className="group-user"
                    title={getGroupUserTitle(item, group)}
                  >
                    <Badge dot={!item.accepted}>
                      <Avatar src={item.avatar} />
                    </Badge>
                    {getGroupUserSpan(item, group)}
                  </div>
                  {item.name !== group.owner &&
                    group.owner === this.props.myself.username && (
                      <Button
                        type="link"
                        size="small"
                        title={item.accepted ? 'Remove' : 'Cancel Invitation'}
                      >
                        <Icon type="close" />
                      </Button>
                    )}
                </List.Item>
              );
            }}
          />
        </div>
        {group.owner === this.props.myself.username && (
          <div className="group-footer">
            <Button type="primary" icon="plus" shape="round" title="Add User" />
          </div>
        )}
      </div>
    );
  }
}

const mapStateToProps = (state: IState) => ({
  myself: state.myself
});

export default connect(mapStateToProps, { deleteGroup })(GroupCard);