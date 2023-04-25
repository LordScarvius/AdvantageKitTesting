package frc.robot.commands.drivetrain;

import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.subsystems.drivetrian.DrivetrainSubsystem;


public class ToTargetCommand extends CommandBase {
	private final DrivetrainSubsystem drivetrainSubsystem;

	public ToTargetCommand(DrivetrainSubsystem drivetrainSubsystem) {
		this.drivetrainSubsystem = drivetrainSubsystem;
		// each subsystem used by the command must be passed into the
		// addRequirements() method (which takes a vararg of Subsystem)
		addRequirements(this.drivetrainSubsystem);
	}

	@Override
	public void initialize() {

	}

	@Override
	public void execute() {

	}

	@Override
	public boolean isFinished() {
		// TODO: Make this return true when this Command no longer needs to run execute()
		return false;
	}

	@Override
	public void end(boolean interrupted) {

	}
}
